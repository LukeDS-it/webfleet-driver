package it.ldsoftware.webfleet.driver

import akka.actor.typed.ActorSystem
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import it.ldsoftware.webfleet.driver.config.AppConfig

// $COVERAGE-OFF$ tested with integration tests
object WebfleetDriverApp extends App with LazyLogging {

  logger.info("Starting Webfleet Driver")

  val systemName = "webfleet-driver-system"

  lazy val appConfig = AppConfig(ConfigFactory.load())

  val guardian = Guardian(
    appConfig.timeout,
    appConfig.serverPort,
    appConfig.jwtConfig,
    appConfig.kafkaProperties,
    appConfig.contentTopic
  )

  val system = ActorSystem[Nothing](guardian, systemName, appConfig.getConfig)

}
// $COVERAGE-ON$
