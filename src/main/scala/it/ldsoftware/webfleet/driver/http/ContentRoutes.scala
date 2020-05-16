package it.ldsoftware.webfleet.driver.http

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import io.circe.generic.auto._
import it.ldsoftware.webfleet.driver.actors.model.{CreationForm, EditingForm, WebContent}
import it.ldsoftware.webfleet.driver.http.model.out.RestError
import it.ldsoftware.webfleet.driver.http.utils.{RouteHelper, UserExtractor}
import it.ldsoftware.webfleet.driver.security.User
import it.ldsoftware.webfleet.driver.service.ContentService
import it.ldsoftware.webfleet.driver.service.model.NoResult

class ContentRoutes(contentService: ContentService, val extractor: UserExtractor)
    extends RouteHelper {

  def routes: Route = path("api" / "v1" / "contents" / Remaining) { rest =>
    val remaining = s"/$rest"
    login { user =>
      getContents(remaining) ~
        createContent(remaining, user) ~
        editContent(remaining, user) ~
        deleteContent(remaining, user)
    }
  }

  private def getContents(remaining: String): Route = get {
    svcCall[WebContent, WebContent](contentService.getContent(remaining), Identity)
  }

  private def createContent(remaining: String, user: User): Route = post {
    entity(as[CreationForm]) { form =>
      svcCall[String, String](contentService.createContent(remaining, form, user), Identity)
    }
  }

  private def editContent(remaining: String, user: User): Route = put {
    entity(as[EditingForm]) { form =>
      svcCall[NoResult, NoResult](contentService.editContent(remaining, form, user), Identity)
    }
  }

  private def deleteContent(remaining: String, user: User): Route = delete {
    if (remaining == "/") {
      complete(StatusCodes.MethodNotAllowed -> RestError("Cannot delete website root"))
    } else {
      svcCall[NoResult, NoResult](contentService.deleteContent(remaining, user), Identity)
    }
  }

}
