package it.ldsoftware.webfleet.driver.http.utils

import it.ldsoftware.webfleet.driver.security.User

import scala.concurrent.Future

trait UserExtractor {
  def extractUser(jwt: String, domain: Option[String]): Future[Option[User]]
}
