package it.ldsoftware.webfleet.driver.service

import scala.concurrent.Future

trait GreeterService {
  def greet(name: String): Future[String]
}
