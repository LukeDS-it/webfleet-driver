package it.ldsoftware.webfleet.driver.service

import it.ldsoftware.webfleet.driver.service.model.ApplicationHealth

import scala.concurrent.Future

trait HealthService {
  def checkHealth: Future[ApplicationHealth]
}
