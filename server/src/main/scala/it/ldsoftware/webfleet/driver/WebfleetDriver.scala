package it.ldsoftware.webfleet.driver

import it.ldsoftware.webfleet.driver.routes.DriverRoutes
import it.ldsoftware.webfleet.driver.services.KafkaService

object WebfleetDriver extends App with DriverRoutes {

  val kafkaService = new KafkaService()

}
