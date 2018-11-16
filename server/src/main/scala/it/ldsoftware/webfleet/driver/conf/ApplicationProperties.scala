package it.ldsoftware.webfleet.driver.conf

import com.typesafe.config.{Config, ConfigFactory}

object ApplicationProperties {
  private lazy val config: Config = ConfigFactory.load()
  lazy val port: Int = config.getInt("webfleet.server.port")
}
