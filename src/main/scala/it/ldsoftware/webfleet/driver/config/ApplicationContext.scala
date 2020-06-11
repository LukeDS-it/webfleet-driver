package it.ldsoftware.webfleet.driver.config

import java.sql.Connection

import com.auth0.jwk.{JwkProvider, JwkProviderBuilder}
import it.ldsoftware.webfleet.driver.database.ExtendedProfile.api._
import it.ldsoftware.webfleet.driver.flows.ContentEventConsumer
import it.ldsoftware.webfleet.driver.flows.consumers.ReadSideEventConsumer
import it.ldsoftware.webfleet.driver.http.utils.{Auth0UserExtractor, UserExtractor}
import it.ldsoftware.webfleet.driver.service.impl.{BasicHealthService, SlickContentReadService}
import it.ldsoftware.webfleet.driver.service.{ContentReadService, HealthService}

import scala.concurrent.ExecutionContext

class ApplicationContext(appConfig: AppConfig)(implicit ec: ExecutionContext) {

  lazy val db: Database = Database.forConfig("slick.db")

  lazy val healthService: HealthService = new BasicHealthService(db)

  lazy val provider: JwkProvider = new JwkProviderBuilder(appConfig.jwtConfig.domain).build()

  lazy val extractor: UserExtractor =
    new Auth0UserExtractor(provider, appConfig.jwtConfig.issuer, appConfig.jwtConfig.audience)

  lazy val readService: ContentReadService = new SlickContentReadService(db)

  lazy val connection: Connection = db.source.createConnection()

  val readSideEventConsumer = new ReadSideEventConsumer(readService)

  lazy val consumers: Seq[ContentEventConsumer] = Seq(readSideEventConsumer)
}
