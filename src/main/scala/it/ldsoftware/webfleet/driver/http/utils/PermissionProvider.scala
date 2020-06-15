package it.ldsoftware.webfleet.driver.http.utils

import scala.concurrent.Future

trait PermissionProvider {
  def getPermissions(domain: String, user: String): Future[Set[String]]
}
