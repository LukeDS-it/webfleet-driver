package it.ldsoftware.webfleet.driver

import akka.actor.typed.ActorSystem
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import it.ldsoftware.webfleet.driver.config.AppConfig

// $COVERAGE-OFF$ tested with integration tests
object WebfleetDriverApp extends App with LazyLogging {

  logger.info("Starting Webfleet Driver")

  lazy val appConfig = AppConfig(ConfigFactory.load())

  val system = ActorSystem[Nothing](
    Guardian(appConfig.timeout, appConfig.serverPort, appConfig.jwtConfig),
    "webfleet-driver-system",
    appConfig.getConfig
  )

}
// $COVERAGE-ON$
