package it.ldsoftware.webfleet.driver.http.utils
import akka.http.scaladsl.HttpExt

import scala.concurrent.Future

class HttpPermissionProvider(wfDomainsUrl: String, http: HttpExt) extends PermissionProvider {
  override def getPermissions(domain: String, user: String): Future[Set[String]] =
    Future.successful(Set())
}
