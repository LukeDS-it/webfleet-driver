package it.ldsoftware.webfleet.driver.http

import akka.http.scaladsl.server.Route
import io.circe.generic.auto._
import it.ldsoftware.webfleet.driver.http.utils.{RouteHelper, UserExtractor}
import it.ldsoftware.webfleet.driver.read.model.ContentRM
import it.ldsoftware.webfleet.driver.service.ContentReadService
import it.ldsoftware.webfleet.driver.service.model.ContentFilter

class SearchRoutes(contents: ContentReadService, val extractor: UserExtractor) extends RouteHelper {

  def routes: Route = path("api" / "v1" / "search") {
    parameterMap { params =>
      svcCall[List[ContentRM]](
        contents
          .search(ContentFilter(params.get("path"), params.get("parent"), params.get("title")))
      )
    }
  }

}
