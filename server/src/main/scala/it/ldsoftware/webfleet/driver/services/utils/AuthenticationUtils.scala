package it.ldsoftware.webfleet.driver.services.utils

import it.ldsoftware.webfleet.api.v1.auth.Principal
import it.ldsoftware.webfleet.api.v1.model.{DriverResult, ForbiddenError}

trait AuthenticationUtils {

  def authorize(principal: Principal, perms: String*)(executable: Principal => DriverResult): DriverResult =
    checkPermissions(principal, perms)
      .map(executable)
      .getOrElse(ForbiddenError)

  private[utils] def checkPermissions(principal: Principal, perms: Seq[String]): Option[Principal] =
    if (perms forall principal.permissions.contains) Some(principal)
    else None
}

object AuthenticationUtils {
  val ScopeAddAggregate = "add:aggregate"
  val ScopeEditAggregate = "edit:aggregate"
  val ScopeMoveAggregate = "move:aggregate"
  val ScopeDeleteAggregate = "delete:aggregate"
}
