package it.ldsoftware.webfleet.api.v1.auth

case class Principal(name: String, permissions: Set[String], jwt: Option[String] = None)
