package it.ldsoftware.webfleet.driver.service

import it.ldsoftware.webfleet.commons.service.model.{ApplicationHealth, ServiceResult}

import scala.concurrent.Future

trait HealthService {
  def checkHealth: Future[ServiceResult[ApplicationHealth]]
}
