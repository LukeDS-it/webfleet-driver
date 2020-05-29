package it.ldsoftware.webfleet.driver.actors

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

object EventProcessor {

  def init(): Unit = {}

  def apply(): Behavior[Nothing] = Behaviors.setup { ctx =>

    Behaviors.same
  }

}


