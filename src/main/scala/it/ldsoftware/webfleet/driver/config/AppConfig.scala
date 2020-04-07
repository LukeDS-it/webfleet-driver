package it.ldsoftware.webfleet.driver.config

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging

case class AppConfig(private val config: Config) extends LazyLogging {

  lazy val serverPort: Int = config.getInt("it.ldsoftware.webfleet.driver.server.port")

}
