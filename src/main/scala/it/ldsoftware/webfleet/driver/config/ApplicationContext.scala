package it.ldsoftware.webfleet.driver.config

import java.sql.Connection

import akka.actor.typed.ActorSystem
import akka.actor.{ActorSystem => ClassicSystem}
import akka.http.scaladsl.Http
import akka.stream.Materializer
import com.auth0.jwk.{JwkProvider, JwkProviderBuilder}
import it.ldsoftware.webfleet.commons.amqp.RabbitMqChannel
import it.ldsoftware.webfleet.commons.flows.EventConsumer
import it.ldsoftware.webfleet.commons.http.{Auth0UserExtractor, PermissionProvider, UserExtractor}
import it.ldsoftware.webfleet.driver.actors.Content.Event
import it.ldsoftware.webfleet.driver.database.ExtendedProfile.api._
import it.ldsoftware.webfleet.driver.flows.JdbcOffsetManager
import it.ldsoftware.webfleet.driver.flows.consumers.{AmqpEventConsumer, ReadSideEventConsumer}
import it.ldsoftware.webfleet.driver.http.utils._
import it.ldsoftware.webfleet.driver.service.impl.{BasicHealthService, SlickContentReadService}
import it.ldsoftware.webfleet.driver.service.{ContentReadService, HealthService}

import scala.concurrent.ExecutionContext

class ApplicationContext(appConfig: AppConfig)(
    implicit ec: ExecutionContext,
    system: ActorSystem[_]
) {

  implicit val classic: ClassicSystem = system.classicSystem

  implicit val mat: Materializer = Materializer(system)

  lazy val db: Database = Database.forConfig("slick.db")

  lazy val healthService: HealthService = new BasicHealthService(db)

  lazy val provider: JwkProvider = new JwkProviderBuilder(appConfig.jwtConfig.domain).build()

  lazy val permissionProvider: PermissionProvider =
    new WebfleetDomainsPermissionProvider(appConfig.wfDomainsUrl, Http(system))

  lazy val extractor: UserExtractor =
    new Auth0UserExtractor(
      provider,
      appConfig.jwtConfig.issuer,
      appConfig.jwtConfig.audience,
      permissionProvider
    )

  lazy val readService: ContentReadService = new SlickContentReadService(db)

  lazy val connection: Connection = db.source.createConnection()

  lazy val offsetManager: JdbcOffsetManager = new JdbcOffsetManager(db)

  lazy val readSideEventConsumer = new ReadSideEventConsumer(readService)

  lazy val amqp = new RabbitMqChannel(appConfig.amqpUrl, appConfig.exchange)

  lazy val amqpEventConsumer = new AmqpEventConsumer(amqp, appConfig.contentDestination)

  lazy val consumers: Seq[EventConsumer[Event]] = Seq(readSideEventConsumer, amqpEventConsumer)
}
