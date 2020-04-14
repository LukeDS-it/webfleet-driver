package it.ldsoftware.webfleet.driver.http

import akka.http.scaladsl.server.Route
import io.circe.generic.auto._
import it.ldsoftware.webfleet.driver.actors.model.{CreationForm, EditingForm, WebContent}
import it.ldsoftware.webfleet.driver.http.utils.{RouteHelper, UserExtractor}
import it.ldsoftware.webfleet.driver.security.User
import it.ldsoftware.webfleet.driver.service.ContentService
import it.ldsoftware.webfleet.driver.service.model.NoResult

class ContentRoutes(contentService: ContentService, val extractor: UserExtractor)
    extends RouteHelper {

  def routes: Route = path("api" / "v1" / "contents") {
    login { user =>
      getContents ~ createContent(user) ~ editContent(user) ~ deleteContent(user)
    }
  }

  private def getContents: Route = get {
    pathEnd {
      svcCall[WebContent, WebContent](contentService.getContent("/"), Identity)
    } ~ path(Remaining) { remaining =>
      svcCall[WebContent, WebContent](contentService.getContent(remaining), Identity)
    }
  }

  private def createContent(user: User): Route = post {
    entity(as[CreationForm]) { form =>
      pathEnd {
        svcCall[String, String](contentService.createContent("/", form, user), Identity)
      } ~ path(Remaining) { remaining =>
        svcCall[String, String](contentService.createContent(remaining, form, user), Identity)
      }
    }
  }

  private def editContent(user: User): Route = put {
    entity(as[EditingForm]) { form =>
      pathEnd {
        svcCall[NoResult, NoResult](contentService.editContent("/", form, user), Identity)
      } ~ path(Remaining) { remaining =>
        svcCall[NoResult, NoResult](
          contentService.editContent(remaining, form, user),
          Identity
        )
      }
    }
  }

  private def deleteContent(user: User): Route = delete {
    path(Remaining) { remaining =>
      svcCall[NoResult, NoResult](contentService.deleteContent(remaining, user), Identity)
    }
  }

}
