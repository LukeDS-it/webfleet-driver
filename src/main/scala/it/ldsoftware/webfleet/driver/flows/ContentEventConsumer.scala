package it.ldsoftware.webfleet.driver.flows

import akka.Done
import it.ldsoftware.webfleet.driver.actors.Content

import scala.concurrent.Future

trait ContentEventConsumer {
  def consume(actorId: String, event: Content.Event): Future[Done]
}
