package it.ldsoftware.webfleet.driver

import java.time.Duration

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal
import akka.persistence.query.PersistenceQuery
import akka.util.Timeout
import it.ldsoftware.webfleet.commons.actors.EventProcessor
import it.ldsoftware.webfleet.commons.flows.EventFlow
import it.ldsoftware.webfleet.driver.actors.Content
import it.ldsoftware.webfleet.driver.config.{AppConfig, ApplicationContext}
import it.ldsoftware.webfleet.driver.database.Migrations
import it.ldsoftware.webfleet.driver.flows.DomainsFlow
import it.ldsoftware.webfleet.driver.http.{AllRoutes, WebfleetServer}
import it.ldsoftware.webfleet.driver.service.impl._

// $COVERAGE-OFF$ Tested with integration tests
object Guardian {
  def apply(appConfig: AppConfig, timeout: Duration, port: Int): Behavior[Nothing] =
    Behaviors.setup[Nothing] { context =>
      implicit val system: ActorSystem[Nothing] = context.system
      import system.executionContext
      implicit val to: Timeout = Timeout.create(timeout)

      val appContext = new ApplicationContext(appConfig)

      new Migrations(appContext.connection).executeMigration()

      val sharding = ClusterSharding(system)

      val readJournal = PersistenceQuery(system.classicSystem)
        .readJournalFor[JdbcReadJournal](JdbcReadJournal.Identifier)

      Content.init(system)
      appContext.consumers
        .map(new EventFlow(Content.Tag, readJournal, appContext.offsetManager, _))
        .foreach(EventProcessor.init(system, _))

      new DomainsFlow(appConfig.domainDestination, appContext.amqp)

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
