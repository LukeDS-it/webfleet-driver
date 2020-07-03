package it.ldsoftware.webfleet.driver.http.utils

import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import com.typesafe.scalalogging.LazyLogging
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.generic.auto._
import it.ldsoftware.webfleet.commons.http.PermissionProvider
import it.ldsoftware.webfleet.driver.http.utils.WebfleetDomainsPermissionProvider.PermissionResponse

import scala.concurrent.{ExecutionContext, Future}

class WebfleetDomainsPermissionProvider(webfleetDomainsUrl: String, http: HttpExt)(
    implicit ec: ExecutionContext,
    mat: Materializer
) extends PermissionProvider
    with FailFastCirceSupport
    with LazyLogging {

  override def getPermissions(domain: String, user: String): Future[Set[String]] = {
    val uri = s"$webfleetDomainsUrl/api/v1/domains/$domain/users/$user/permissions"
    logger.info(s"Asking to $webfleetDomainsUrl permissions of $user in $domain")
    http
      .singleRequest(HttpRequest(uri = uri))
      .flatMap(Unmarshal(_).to[PermissionResponse])
      .map(r => r.permissions)
  }

}

object WebfleetDomainsPermissionProvider {
  case class PermissionResponse(permissions: Set[String])
}
