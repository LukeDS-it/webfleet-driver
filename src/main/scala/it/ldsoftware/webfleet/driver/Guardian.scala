package it.ldsoftware.webfleet.driver

import java.time.Duration

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import com.auth0.jwk.JwkProviderBuilder
import it.ldsoftware.webfleet.driver.actors.Content
import it.ldsoftware.webfleet.driver.config.JwtConfig
import it.ldsoftware.webfleet.driver.http.utils.Auth0UserExtractor
import it.ldsoftware.webfleet.driver.http.{AllRoutes, WebfleetServer}
import it.ldsoftware.webfleet.driver.service.impl.{ActorContentService, BasicHealthService}
import slick.jdbc.PostgresProfile.api._

// $COVERAGE-OFF$ Tested with integration tests
object Guardian {
  def apply(timeout: Duration, port: Int, jwtConfig: JwtConfig): Behavior[Nothing] =
    Behaviors.setup[Nothing] { context =>
      implicit val system: ActorSystem[Nothing] = context.system
      import system.executionContext

      val sharding = ClusterSharding(system)

      val db = Database.forConfig("slick.db")

      Content.init(system)

      val healthService = new BasicHealthService(db)
      val provider = new JwkProviderBuilder(jwtConfig.domain).build()
      val extractor = new Auth0UserExtractor(provider, jwtConfig.issuer, jwtConfig.audience)

      val contentService = new ActorContentService(timeout, sharding)

      val routes = new AllRoutes(extractor, contentService, healthService).routes
      new WebfleetServer(routes, port, context.system).start()

      Behaviors.empty
    }
}
// $COVERAGE-ON$
