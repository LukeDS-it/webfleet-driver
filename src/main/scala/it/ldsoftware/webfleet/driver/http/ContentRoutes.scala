package it.ldsoftware.webfleet.driver.http

import akka.http.scaladsl.server.Route
import it.ldsoftware.webfleet.driver.http.utils.RouteHelper

trait ContentRoutes extends RouteHelper {

  def routes: Route = path("api" / "v1" / "contents") {
    get {
      pathEnd {
        complete()
      } ~ path(Remaining) { remaining => complete() }
    } ~
      post {
        complete()
      } ~
      put {
        complete()
      } ~ delete {
      complete()
    }
  }

}
