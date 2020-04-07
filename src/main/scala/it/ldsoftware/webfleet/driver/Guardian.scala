package it.ldsoftware.webfleet.driver

import java.time.Duration

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import it.ldsoftware.webfleet.driver.actors.GreeterActor
import it.ldsoftware.webfleet.driver.http.{BasicRoutes, WebfleetServer}
import it.ldsoftware.webfleet.driver.service.impl.ActorGreeterService

object Guardian {
  def apply(timeout: Duration, port: Int): Behavior[Nothing] = {
    Behaviors.setup[Nothing] { context =>
      implicit val system: ActorSystem[Nothing] = context.system
      import system.executionContext

      GreeterActor.init(system)

      val greeterService = new ActorGreeterService(timeout)

      val routes = new BasicRoutes(greeterService).routes
      new WebfleetServer(routes, port, context.system).start()

      Behaviors.empty
    }
  }
}
