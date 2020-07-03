package it.ldsoftware.webfleet.driver.http

import akka.http.scaladsl.server.Route
import io.circe.generic.auto._
import it.ldsoftware.webfleet.commons.http.{RouteHelper, UserExtractor}
import it.ldsoftware.webfleet.commons.service.model.ApplicationHealth
import it.ldsoftware.webfleet.driver.service.HealthService

class HealthRoutes(healthService: HealthService, val extractor: UserExtractor) extends RouteHelper {
  def routes: Route = path("health") {
    get {
      svcCall[ApplicationHealth](healthService.checkHealth)
    }
  }
}
