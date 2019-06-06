package it.ldsoftware.webfleet.driver.services.utils

trait PrincipalExtractor {
  def extractPrincipal(jwt: String): Option[Principal]
}
