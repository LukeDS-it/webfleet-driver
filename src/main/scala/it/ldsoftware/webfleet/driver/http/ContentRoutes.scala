package it.ldsoftware.webfleet.driver.http

import akka.http.scaladsl.server.Route
import io.circe.generic.auto._
import it.ldsoftware.webfleet.driver.actors.model.{CreationForm, EditingForm, WebContent}
import it.ldsoftware.webfleet.driver.http.utils.RouteHelper
import it.ldsoftware.webfleet.driver.security.User
import it.ldsoftware.webfleet.driver.service.ContentService
import it.ldsoftware.webfleet.driver.service.model.NoResult

class ContentRoutes(contentService: ContentService) extends RouteHelper {

  def routes: Route = path("api" / "v1" / "contents") {
    getContents ~ createContent(null) ~ editContent(null) ~ deleteContent(null)
  }

  private def getContents: Route = get {
    pathEnd {
      completeWith[WebContent, WebContent](contentService.getContent("/"), Identity)
    } ~ path(Remaining) { remaining =>
      completeWith[WebContent, WebContent](contentService.getContent(remaining), Identity)
    }
  }

  private def createContent(user: User): Route = post {
    entity(as[CreationForm]) { form =>
      pathEnd {
        completeWith[String, String](contentService.createContent("/", form, user), Identity)
      } ~ path(Remaining) { remaining =>
        completeWith[String, String](contentService.createContent(remaining, form, user), Identity)
      }
    }
  }

  private def editContent(user: User): Route = put {
    entity(as[EditingForm]) { form =>
      pathEnd {
        completeWith[NoResult, NoResult](contentService.editContent("/", form, user), Identity)
      } ~ path(Remaining) { remaining =>
        completeWith[NoResult, NoResult](contentService.editContent(remaining, form, user), Identity)
      }
    }
  }

  private def deleteContent(user: User): Route = delete {
    path(Remaining) { remaining =>
      completeWith[NoResult, NoResult](contentService.deleteContent(remaining, user), Identity)
    }
  }

}
