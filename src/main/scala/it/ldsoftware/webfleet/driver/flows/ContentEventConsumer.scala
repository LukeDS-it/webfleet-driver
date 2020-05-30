package it.ldsoftware.webfleet.driver.flows

import it.ldsoftware.webfleet.driver.actors.Content

trait ContentEventConsumer {
  def consume(actorId: String, event: Content.Event): Unit
}
