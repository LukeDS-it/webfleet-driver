package it.ldsoftware.webfleet.driver.actors.model

case class ValidationError(field: String, error: String, code: String)
