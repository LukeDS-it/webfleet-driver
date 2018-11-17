package it.ldsoftware.webfleet.driver.services.utils

import java.util

import io.jsonwebtoken.{Claims, Jwts}
import it.ldsoftware.webfleet.api.v1.model.{DriverResult, ForbiddenError}
import it.ldsoftware.webfleet.driver.conf.ApplicationProperties

import scala.collection.JavaConverters._
import scala.util.Try

trait AuthenticationUtils {

  def authorize(jwt: String, perms: String*)(executable: Principal => DriverResult): DriverResult =
    extractPrincipal(jwt)
      .flatMap(checkPermissions(_, perms))
      .map(executable)
      .getOrElse(ForbiddenError)

  private[utils] def extractPrincipal(jwt: String): Option[Principal] = Try {
    Jwts.parser()
      .setSigningKey(ApplicationProperties.jwtSigningKey)
      .parseClaimsJws(jwt)
      .getBody
  }.map { token =>
    Some(Principal(token.getSubject, extractPermissions(token)))
  }.getOrElse(None)

  def checkPermissions(principal: Principal, perms: Seq[String]): Option[Principal] =
    if (perms forall principal.permissions.contains) Some(principal)
    else None

  def extractPermissions(token: Claims): Set[String] = {
    val list = token.get[util.ArrayList[String]]("webfleet-driver-roles", classOf[util.ArrayList[String]])
    list.asScala.toSet
  }
}

object AuthenticationUtils {
  val RoleAddAggregate = "ADD_AGGREGATE"
  val RoleEditAggregate = "EDIT_AGGREGATE"
  val RoleMoveAggregate = "MOVE_AGGREGATE"
  val RoleDeleteAggregate = "DELETE_AGGREGATE"
}
