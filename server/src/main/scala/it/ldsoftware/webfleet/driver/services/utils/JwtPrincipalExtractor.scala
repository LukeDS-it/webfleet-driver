package it.ldsoftware.webfleet.driver.services.utils

import java.util

import io.jsonwebtoken.{Claims, Jwts}
import it.ldsoftware.webfleet.driver.conf.ApplicationProperties

import scala.collection.JavaConverters._
import scala.util.Try

class JwtPrincipalExtractor extends PrincipalExtractor {

  override def extractPrincipal(jwt: String): Option[Principal] = Try {
    Jwts.parser()
      .setSigningKey(ApplicationProperties.jwtSigningKey)
      .parseClaimsJws(jwt)
      .getBody
  }.map { token =>
    Some(Principal(token.getSubject, extractPermissions(token)))
  }.getOrElse(None)

  private def extractPermissions(token: Claims): Set[String] = {
    val list = token.get[util.ArrayList[String]]("webfleet-driver-roles", classOf[util.ArrayList[String]])
    list.asScala.toSet
  }
}
