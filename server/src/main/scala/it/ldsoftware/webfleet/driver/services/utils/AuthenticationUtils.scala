package it.ldsoftware.webfleet.driver.services.utils

import io.jsonwebtoken.Jwts
import it.ldsoftware.webfleet.api.v1.model.{DriverResult, ForbiddenError}
import it.ldsoftware.webfleet.driver.conf.ApplicationProperties

import scala.util.Try

trait AuthenticationUtils {

  def authorize(jwt: String, perms: String*)(executable: Principal => DriverResult): DriverResult =
    extractPrincipal(jwt)
      .flatMap(checkPermissions(_, perms))
      .map(executable)
      .getOrElse(ForbiddenError)

  def extractPrincipal(jwt: String): Option[Principal] = Try {
    Jwts.parser()
      .setSigningKey(ApplicationProperties.jwtSigningKey)
      .parseClaimsJws(jwt)
      .getBody
  }.map { token =>
    Some(Principal(
      token.getSubject,
      token.get[List[String]]("webfleet-driver-roles", Class[List[String]])
    ))
  }.getOrElse(None)

  def checkPermissions(principal: Principal, perms: Seq[String]): Option[Principal] =
    if (perms forall (principal.permissions.contains(_))) Some(principal)
    else None
}

object AuthenticationUtils {
  val RoleAddAggregate = "ADD_AGGREGATE"
  val RoleEditAggregate = "EDIT_AGGREGATE"
  val RoleMoveAggregate = "MOVE_AGGREGATE"
  val RoleDeleteAggregate = "DELETE_AGGREGATE"
}
