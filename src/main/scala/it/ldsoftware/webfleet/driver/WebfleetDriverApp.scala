package it.ldsoftware.webfleet.driver

import akka.actor.typed.ActorSystem
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import it.ldsoftware.webfleet.driver.config.AppConfig

object WebfleetDriverApp extends App with LazyLogging {

  logger.info("Starting Webfleet Driver")

  lazy val appConfig = AppConfig(ConfigFactory.load())

  val system = ActorSystem[Nothing](
    Guardian(appConfig.timeout, appConfig.serverPort),
    "webfleet-driver-system",
    appConfig.getConfig
  )

}
