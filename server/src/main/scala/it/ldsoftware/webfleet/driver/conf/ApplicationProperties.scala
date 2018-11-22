package it.ldsoftware.webfleet.driver.conf

import java.security.Key
import java.util.Base64
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec

import com.typesafe.config.{Config, ConfigFactory}
import scalikejdbc.ConnectionPoolSettings

object ApplicationProperties {
  val PK_HEADER = "-----BEGIN PUBLIC KEY-----"
  val PK_FOOTER = "-----END PUBLIC KEY-----"
  private lazy val config: Config = ConfigFactory.load()

  lazy val port: Int = config.getInt("webfleet.server.port")

  lazy val databaseUrl: String = {
    val url = config.getString("webfleet.database.url")
    if (url.startsWith("jdbc:")) url
    else s"jdbc:$url"
  }

  lazy val databaseUser: String = config.getString("webfleet.database.user")
  lazy val databasePass: String = config.getString("webfleet.database.pass")
  lazy val databaseDriver: String = config.getString("webfleet.database.driver")

  lazy val connectionPoolSettings = ConnectionPoolSettings(
    initialSize = config.getInt("webfleet.database.pool.size"),
    maxSize = config.getInt("webfleet.database.pool.max"),
    connectionTimeoutMillis = config.getInt("webfleet.database.pool.timeout"),
    validationQuery = config.getString("webfleet.database.pool.validation")
  )

  lazy val jwtSigningKey: Key = {
    val publicCert = config.getString("webfleet.security.jwt-public-key")
    val der = if (publicCert.startsWith("----")) publicCert.substring(PK_HEADER.length, publicCert.indexOf(PK_FOOTER))
    else publicCert

    val keyBytes = Base64.getDecoder.decode(der.getBytes)

    KeyFactory
      .getInstance("RSA")
      .generatePublic(new X509EncodedKeySpec(keyBytes))
  }

}
