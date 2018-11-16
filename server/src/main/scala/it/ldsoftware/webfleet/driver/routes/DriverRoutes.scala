package it.ldsoftware.webfleet.driver.routes

import akka.http.scaladsl.server.Route

trait DriverRoutes extends HealthRoutes with AggregateRoutes with ContentRoutes with EventRoutes with UpdateRoutes {

  def routes: Route = healthRoute
}
