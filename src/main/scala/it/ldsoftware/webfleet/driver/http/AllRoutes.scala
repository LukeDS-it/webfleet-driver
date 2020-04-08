package it.ldsoftware.webfleet.driver.http

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import io.circe.generic.auto._
import it.ldsoftware.webfleet.driver.http.model.in.NamedEntity
import it.ldsoftware.webfleet.driver.http.utils.RouteHelper
import it.ldsoftware.webfleet.driver.service.{GreeterService, HealthService}

class AllRoutes(greeterService: GreeterService, healthService: HealthService) extends RouteHelper {

  def routes: Route =
    path("") {
      get {
        complete("Hello world!")
      } ~ post {
        entity(as[NamedEntity]) { named => completeWith[String, String](greeterService.greet(named.name), x => x) }
      }
    } ~ path("health") {
      get {
        onSuccess(healthService.checkHealth) { resp =>
          val status = if (resp.ok) StatusCodes.OK else StatusCodes.InternalServerError
          complete(status -> resp)
        }
      }
    }

}
