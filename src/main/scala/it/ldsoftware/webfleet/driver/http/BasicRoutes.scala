package it.ldsoftware.webfleet.driver.http

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.typesafe.scalalogging.StrictLogging
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.generic.auto._
import it.ldsoftware.webfleet.driver.http.model.NamedEntity
import it.ldsoftware.webfleet.driver.service.GreeterService

class BasicRoutes(greeterService: GreeterService) extends StrictLogging with FailFastCirceSupport {

  def routes: Route = path("") {
    get {
      complete("Hello world!")
    } ~ post {
      entity(as[NamedEntity]) { named =>
        onSuccess(greeterService.greet(named.name)) { resp =>
          complete(resp)
        }
      }
    }
  }

}
