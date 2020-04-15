package it.ldsoftware.webfleet.driver.config

import java.time.Duration

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging

// $COVERAGE-OFF$
case class AppConfig(private val config: Config) extends LazyLogging {

  lazy val serverPort: Int = config.getInt("it.ldsoftware.webfleet.driver.server.port")

  lazy val timeout: Duration = config.getDuration("it.ldsoftware.webfleet.driver.timeout")

  lazy val jwtConfig: JwtConfig = JwtConfig(
    config.getString("it.ldsoftware.webfleet.driver.auth0.audience"),
    config.getString("it.ldsoftware.webfleet.driver.auth0.domain"),
    config.getString("it.ldsoftware.webfleet.driver.auth0.issuer")
  )

  def getConfig: Config = config

}

case class JwtConfig(audience: String, domain: String, issuer: String)
// $COVERAGE-ON$
