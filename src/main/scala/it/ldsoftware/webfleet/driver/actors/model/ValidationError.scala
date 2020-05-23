package it.ldsoftware.webfleet.driver.actors.model

import it.ldsoftware.webfleet.driver.actors.serialization.CborSerializable

case class ValidationError(field: String, error: String, code: String) extends CborSerializable
