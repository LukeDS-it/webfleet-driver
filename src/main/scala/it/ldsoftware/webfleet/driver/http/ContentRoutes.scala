package it.ldsoftware.webfleet.driver.http

import akka.http.scaladsl.server.Route
import it.ldsoftware.webfleet.driver.http.utils.RouteHelper

trait ContentRoutes extends RouteHelper {

  def routes: Route = path("api" / "v1" / "contents") {
    getContents ~ createContent ~ editContent ~ delContent
  }

  private def getContents: Route = get {
    pathEnd {
      complete("ok")
    } ~ path(Remaining) { remaining => complete(remaining) }
  }

  private def createContent: Route = post {
    pathEnd {
      complete("ok")
    } ~ path(Remaining) { remaining => complete(remaining) }
  }

  private def editContent: Route = put {
    pathEnd {
      complete("ok")
    } ~ path(Remaining) { remaining => complete(remaining) }
  }

  private def delContent: Route = delete {
    path(Remaining) { remaining => complete(remaining) }
  }

}
