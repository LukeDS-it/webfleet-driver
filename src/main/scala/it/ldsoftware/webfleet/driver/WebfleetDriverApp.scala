package it.ldsoftware.webfleet.driver

import akka.actor.typed.ActorSystem
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import it.ldsoftware.webfleet.driver.config.{AppConfig, ApplicationContext}
import it.ldsoftware.webfleet.driver.database.Migrations

import scala.concurrent.ExecutionContext

// $COVERAGE-OFF$ tested with integration tests
object WebfleetDriverApp extends App with LazyLogging {

  logger.info("Starting Webfleet Driver")

  implicit val ec: ExecutionContext = ExecutionContext.global

  lazy val appConfig = AppConfig(ConfigFactory.load())

  lazy val appContext = new ApplicationContext(appConfig)

  new Migrations(appContext.connection).executeMigration()

  val system = ActorSystem[Nothing](
    Guardian(appContext, appConfig.timeout, appConfig.serverPort),
    "webfleet-driver-system",
    appConfig.getConfig
  )

}
// $COVERAGE-ON$
