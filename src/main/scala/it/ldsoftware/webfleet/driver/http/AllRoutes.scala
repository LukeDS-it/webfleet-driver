package it.ldsoftware.webfleet.driver.http

import akka.http.scaladsl.server.{Directives, Route}
import it.ldsoftware.webfleet.driver.http.utils.UserExtractor
import it.ldsoftware.webfleet.driver.service.{ContentReadService, ContentService, HealthService}

// $COVERAGE-OFF$ specific route tests exist, this is just an aggregate
class AllRoutes(
    extractor: UserExtractor,
    contentService: ContentService,
    healthService: HealthService,
    readService: ContentReadService
) extends Directives {

  def routes: Route =
    new ContentRoutes(contentService, extractor).routes ~
      new HealthRoutes(healthService, extractor).routes ~
      new SearchRoutes(readService, extractor).routes

}
// $COVERAGE-ON$
