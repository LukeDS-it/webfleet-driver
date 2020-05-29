package it.ldsoftware.webfleet.driver.actors

import akka.actor.typed.scaladsl.Behaviors

object EventProcessor {

  def init(): Unit = {}

  def apply() = Behaviors.setup { ctx => Behaviors.same }

}
