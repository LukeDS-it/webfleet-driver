package it.ldsoftware.webfleet.driver.http

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.typesafe.scalalogging.StrictLogging
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import it.ldsoftware.webfleet.driver.http.model.NamedEntity
import io.circe.generic.auto._

trait BasicRoutes extends StrictLogging with FailFastCirceSupport {

  def routes: Route = path("") {
    get {
      complete("Hello world!")
    } ~ post {
      entity(as[NamedEntity]) { named =>
        complete(s"Hello ${named.name}")
      }
    }
  }

}
