package service

import client.RouteService
import database.entity.Item
import database.entity.Route
import database.entity.RouteStop
import database.entity.Stop
import database.table.Routes
import database.table.Stops
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

object CollectingService {
    suspend fun collect() {
        val routes = RouteService.getAllRoutes()

        transaction {
            for (route in routes) {
                Route.new(route.id) {
                    name = route.shortName
                    transportType = route.transportType
                }
            }
        }
        for (route in routes) {
            val routeInfo = RouteService.getRouteById(route.id)
            for (direction in routeInfo.directions) {
                for (item in direction.items) {

                    transaction {

                        val start = Stop.findById(item.startStopId) ?: Stop.new(item.startStopId) {
                            latitude = item.startStopLatitude
                            longitude = item.startStopLongitude
                        }
                        val finish = Stop.findById(item.finishStopId) ?: Stop.new(item.finishStopId) {
                            latitude = item.finishStopLatitude
                            longitude = item.finishStopLongitude
                        }
                        val routeEntity = Route[route.id]
                        Item.new {
                            order = item.order
                            this.direction = direction.direction
                            shape = item.shape
                            this.route = routeEntity
                            this.from = start
                            this.to = finish
                        }

                        if (start !in routeEntity.stops) RouteStop.new {
                            this.route = Route[route.id]
                            this.stop = start
                        }
                        if (finish !in routeEntity.stops) RouteStop.new {
                            this.route = Route[route.id]
                            this.stop = finish
                        }
                    }
                }
            }
        }

        transaction {
             val countStops = Stops.selectAll().count()
             println("$countStops")
        }
    }
}