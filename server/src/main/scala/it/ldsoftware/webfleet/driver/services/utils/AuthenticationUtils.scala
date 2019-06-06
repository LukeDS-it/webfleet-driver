package it.ldsoftware.webfleet.driver.services.utils

import it.ldsoftware.webfleet.api.v1.model.{DriverResult, ForbiddenError}

trait AuthenticationUtils {

  def extractor: PrincipalExtractor

  def authorize(jwt: String, perms: String*)(executable: Principal => DriverResult): DriverResult =
    extractor
      .extractPrincipal(jwt)
      .flatMap(checkPermissions(_, perms))
      .map(executable)
      .getOrElse(ForbiddenError)

  private[utils] def checkPermissions(principal: Principal, perms: Seq[String]): Option[Principal] =
    if (perms forall principal.permissions.contains) Some(principal)
    else None
}

object AuthenticationUtils {
  val RoleAddAggregate = "ADD_AGGREGATE"
  val RoleEditAggregate = "EDIT_AGGREGATE"
  val RoleMoveAggregate = "MOVE_AGGREGATE"
  val RoleDeleteAggregate = "DELETE_AGGREGATE"
}
