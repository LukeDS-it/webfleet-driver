package it.ldsoftware.webfleet.driver

import java.time.Duration

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import it.ldsoftware.webfleet.driver.actors.GreeterActor
import it.ldsoftware.webfleet.driver.http.{AllRoutes, WebfleetServer}
import it.ldsoftware.webfleet.driver.service.impl.{ActorGreeterService, BasicHealthService}
import slick.jdbc.PostgresProfile.api._

object Guardian {
  def apply(timeout: Duration, port: Int): Behavior[Nothing] = {
    Behaviors.setup[Nothing] { context =>
      implicit val system: ActorSystem[Nothing] = context.system
      import system.executionContext

      val db = Database.forConfig("slick.db")

      GreeterActor.init(system)

      val greeterService = new ActorGreeterService(timeout)
      val healthService = new BasicHealthService(db)

      val routes = new AllRoutes(greeterService, healthService).routes
      new WebfleetServer(routes, port, context.system).start()

      Behaviors.empty
    }
  }
}
