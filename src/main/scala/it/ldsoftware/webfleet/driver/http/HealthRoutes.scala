package it.ldsoftware.webfleet.driver.http

import akka.http.scaladsl.server.Route
import io.circe.generic.auto._
import it.ldsoftware.webfleet.driver.http.utils.{RouteHelper, UserExtractor}
import it.ldsoftware.webfleet.driver.service.HealthService
import it.ldsoftware.webfleet.driver.service.model.ApplicationHealth

class HealthRoutes(healthService: HealthService, val extractor: UserExtractor) extends RouteHelper {
  def routes: Route = path("health") {
    get {
      svcCall[ApplicationHealth](healthService.checkHealth)
    }
  }
}
