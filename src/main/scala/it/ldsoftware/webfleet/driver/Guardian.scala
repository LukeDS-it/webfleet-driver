package it.ldsoftware.webfleet.driver

import java.time.Duration

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal
import akka.persistence.query.PersistenceQuery
import it.ldsoftware.webfleet.driver.actors.{Content, EventProcessor}
import it.ldsoftware.webfleet.driver.config.ApplicationContext
import it.ldsoftware.webfleet.driver.flows.ContentFlow
import it.ldsoftware.webfleet.driver.http.{AllRoutes, WebfleetServer}
import it.ldsoftware.webfleet.driver.service.impl._

// $COVERAGE-OFF$ Tested with integration tests
object Guardian {
  def apply(appContext: ApplicationContext, timeout: Duration, port: Int): Behavior[Nothing] =
    Behaviors.setup[Nothing] { context =>
      implicit val system: ActorSystem[Nothing] = context.system
      import system.executionContext

      val sharding = ClusterSharding(system)

      val readJournal = PersistenceQuery(system.classicSystem)
        .readJournalFor[JdbcReadJournal](JdbcReadJournal.Identifier)

      val flow = new ContentFlow(readJournal, appContext.db, appContext.consumers)

      Content.init(system)
      EventProcessor.init(system, flow)

      val contentService = new ActorContentService(timeout, sharding)

      val routes = new AllRoutes(
        appContext.extractor,
        contentService,
        appContext.healthService,
        appContext.readService
      ).routes

      new WebfleetServer(routes, port, context.system).start()

      Behaviors.empty
    }
}
// $COVERAGE-ON$
