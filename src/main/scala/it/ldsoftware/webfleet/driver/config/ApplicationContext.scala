package it.ldsoftware.webfleet.driver.config

import java.sql.Connection

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import com.auth0.jwk.{JwkProvider, JwkProviderBuilder}
import it.ldsoftware.webfleet.driver.database.ExtendedProfile.api._
import it.ldsoftware.webfleet.driver.flows.consumers.{KafkaEventConsumer, ReadSideEventConsumer}
import it.ldsoftware.webfleet.driver.flows.{ContentEventConsumer, OffsetManager}
import it.ldsoftware.webfleet.driver.http.utils._
import it.ldsoftware.webfleet.driver.service.impl.{BasicHealthService, SlickContentReadService}
import it.ldsoftware.webfleet.driver.service.{ContentReadService, HealthService}
import org.apache.kafka.clients.producer.KafkaProducer

import scala.concurrent.ExecutionContext

class ApplicationContext(appConfig: AppConfig)(
    implicit ec: ExecutionContext,
    system: ActorSystem[_]
) {

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

  lazy val offsetManager: OffsetManager = new OffsetManager(db)

  lazy val readSideEventConsumer = new ReadSideEventConsumer(readService)

  lazy val kafkaEventConsumer = new KafkaEventConsumer(
    new KafkaProducer[String, String](appConfig.producerProperties),
    appConfig.contentTopic
  )

  lazy val consumers: Seq[ContentEventConsumer] = Seq(readSideEventConsumer, kafkaEventConsumer)
}
