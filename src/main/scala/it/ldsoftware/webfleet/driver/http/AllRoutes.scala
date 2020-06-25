package it.ldsoftware.webfleet.driver.http

import akka.http.scaladsl.server.Route
import it.ldsoftware.webfleet.driver.http.utils.{CORSHelper, UserExtractor}
import it.ldsoftware.webfleet.driver.service.{ContentReadService, ContentService, HealthService}

// $COVERAGE-OFF$ specific route tests exist, this is just an aggregate
class AllRoutes(
    extractor: UserExtractor,
    contentService: ContentService,
    healthService: HealthService,
    readService: ContentReadService
) extends CORSHelper {

  def routes: Route =
    corsHandler {
      new ContentRoutes(contentService, extractor).routes ~
        new HealthRoutes(healthService, extractor).routes ~
        new SearchRoutes(readService, extractor).routes
    }

}
// $COVERAGE-ON$
