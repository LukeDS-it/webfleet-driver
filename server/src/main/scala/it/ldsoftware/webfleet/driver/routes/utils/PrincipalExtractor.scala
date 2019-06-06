package it.ldsoftware.webfleet.driver.routes.utils

import it.ldsoftware.webfleet.api.v1.auth.Principal

trait PrincipalExtractor {
  def extractPrincipal(jwt: String): Option[Principal]
}
