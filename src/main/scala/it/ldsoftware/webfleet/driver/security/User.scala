package it.ldsoftware.webfleet.driver.security

case class User(name: String, permissions: Set[String] = Set(), jwt: Option[String] = None)
