package it.ldsoftware.webfleet.driver.routes

import akka.http.scaladsl.server.Route
import it.ldsoftware.webfleet.api.v1.model.Aggregate
import it.ldsoftware.webfleet.api.v1.service.AggregateDriverV1
import it.ldsoftware.webfleet.driver.routes.utils.{AggregateFormatting, RouteUtils}

trait AggregateRoutes extends RouteUtils with AggregateFormatting {

  def aggregateService: AggregateDriverV1

  def aggregateRoutes: Route = authenticateOAuth2("realm", authenticator) { jwt =>
    path("api" / "v1" / "aggregates") {
      post {
        entity(as[Aggregate]) { agg =>
          completeFrom(aggregateService.addAggregate(None, agg, jwt))
        }
      } ~
        path(Segment) { id =>
          post {
            entity(as[Aggregate]) { agg =>
              completeFrom(aggregateService.addAggregate(Some(id), agg, jwt))
            }
          } ~
            put {
              entity(as[Aggregate]) { agg =>
                completeFrom(aggregateService.editAggregate(id, agg, jwt))
              }
            } ~
            delete {
              completeFrom(aggregateService.deleteAggregate(id, jwt))
            } ~
            path("move" / Segment) { dest =>
              completeFrom(aggregateService.moveAggregate(id, dest, jwt))
            }
        }

    }
  }

}

object AggregateRoutes {
  val aggregatePath = "/api/v1/aggregates"
}