package it.ldsoftware.webfleet.driver.service

import it.ldsoftware.webfleet.driver.service.model.ServiceResult

import scala.concurrent.Future

trait GreeterService {
  def greet(name: String): Future[ServiceResult[String]]
}
