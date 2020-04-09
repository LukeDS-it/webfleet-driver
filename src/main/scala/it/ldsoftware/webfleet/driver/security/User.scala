package it.ldsoftware.webfleet.driver.security

case class User(name: String, permissions: List[String] = List(), jwt: Option[String] = None)
