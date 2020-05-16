package it.ldsoftware.webfleet.driver.http.utils

import it.ldsoftware.webfleet.driver.security.User

trait UserExtractor {
  def extractUser(jwt: String): Option[User]
}
