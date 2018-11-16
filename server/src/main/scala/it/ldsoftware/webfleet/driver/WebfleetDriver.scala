package it.ldsoftware.webfleet.driver

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import it.ldsoftware.webfleet.driver.conf.ApplicationProperties
import it.ldsoftware.webfleet.driver.routes.DriverRoutes
import it.ldsoftware.webfleet.driver.services.KafkaService

object WebfleetDriver extends App with DriverRoutes {

  val kafkaService = new KafkaService()

  implicit val system: ActorSystem = ActorSystem("webfleet-driver")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val port = ApplicationProperties.port

  println(s"Starting webfleet-driver on port $port")

  Http().bindAndHandle(routes, "0.0.0.0", port)

}
