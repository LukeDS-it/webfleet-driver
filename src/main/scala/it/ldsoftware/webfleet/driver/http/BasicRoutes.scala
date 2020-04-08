package it.ldsoftware.webfleet.driver.http

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.typesafe.scalalogging.StrictLogging
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.generic.auto._
import it.ldsoftware.webfleet.driver.http.model.NamedEntity
import it.ldsoftware.webfleet.driver.service.{GreeterService, HealthService}

class BasicRoutes(greeterService: GreeterService, healthService: HealthService)
    extends StrictLogging
    with FailFastCirceSupport {

  def routes: Route =
    path("") {
      get {
        complete("Hello world!")
      } ~ post {
        entity(as[NamedEntity]) { named => onSuccess(greeterService.greet(named.name)) { resp => complete(resp) } }
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
