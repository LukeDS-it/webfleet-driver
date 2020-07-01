package it.ldsoftware.webfleet.driver.util

import it.ldsoftware.webfleet.driver.actors.serialization.CborSerializable

case class RabbitEnvelope[T](entityId: String, content: T) extends CborSerializable
