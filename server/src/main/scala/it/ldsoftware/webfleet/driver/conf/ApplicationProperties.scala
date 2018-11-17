package it.ldsoftware.webfleet.driver.conf

import java.security.Key
import java.util.Base64
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec

import com.typesafe.config.{Config, ConfigFactory}

object ApplicationProperties {
  val PK_HEADER = "-----BEGIN PUBLIC KEY-----"
  val PK_FOOTER = "-----END PUBLIC KEY-----"
  private lazy val config: Config = ConfigFactory.load()

  lazy val port: Int = config.getInt("webfleet.server.port")

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
