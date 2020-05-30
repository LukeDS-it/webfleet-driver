package it.ldsoftware.webfleet.driver

import java.time.Duration

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal
import akka.persistence.query.PersistenceQuery
import com.auth0.jwk.JwkProviderBuilder
import it.ldsoftware.webfleet.driver.actors.{Content, EventProcessor}
import it.ldsoftware.webfleet.driver.config.JwtConfig
import it.ldsoftware.webfleet.driver.flows.ContentFlow
import it.ldsoftware.webfleet.driver.flows.consumers.ReadSideEventConsumer
import it.ldsoftware.webfleet.driver.http.utils.Auth0UserExtractor
import it.ldsoftware.webfleet.driver.http.{AllRoutes, WebfleetServer}
import it.ldsoftware.webfleet.driver.service.impl._
import slick.jdbc.PostgresProfile.api._

// $COVERAGE-OFF$ Tested with integration tests
object Guardian {
  def apply(timeout: Duration, port: Int, jwtConfig: JwtConfig): Behavior[Nothing] =
    Behaviors.setup[Nothing] { context =>
      implicit val system: ActorSystem[Nothing] = context.system
      import system.executionContext

      val sharding = ClusterSharding(system)

      val db = Database.forConfig("slick.db")
      val readJournal = PersistenceQuery(system.classicSystem)
        .readJournalFor[JdbcReadJournal](JdbcReadJournal.Identifier)

      val readService = new SlickContentReadService(db)
      val readSideEventConsumer = new ReadSideEventConsumer(readService)

      val flow = new ContentFlow(readJournal, db, Seq(readSideEventConsumer))

      Content.init(system)
      EventProcessor.init(system, flow)

      val healthService = new BasicHealthService(db)
      val provider = new JwkProviderBuilder(jwtConfig.domain).build()
      val extractor = new Auth0UserExtractor(provider, jwtConfig.issuer, jwtConfig.audience)

      val contentService = new ActorContentService(timeout, sharding)

      val routes = new AllRoutes(extractor, contentService, healthService, readService).routes
      new WebfleetServer(routes, port, context.system).start()

      Behaviors.empty
    }
}
// $COVERAGE-ON$
