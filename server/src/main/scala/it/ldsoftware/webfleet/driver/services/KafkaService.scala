package it.ldsoftware.webfleet.driver.services

import akka.http.scaladsl.model.{StatusCode, StatusCodes}

class KafkaService {
  def getHealth: StatusCode = StatusCodes.OK
}
