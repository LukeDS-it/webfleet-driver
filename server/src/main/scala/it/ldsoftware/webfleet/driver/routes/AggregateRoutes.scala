package it.ldsoftware.webfleet.driver.routes

import akka.http.scaladsl.server.Route
import it.ldsoftware.webfleet.api.v1.model.Aggregate
import it.ldsoftware.webfleet.api.v1.service.AggregateDriverV1
import it.ldsoftware.webfleet.driver.routes.utils.{AggregateFormatting, RouteUtils}
import it.ldsoftware.webfleet.driver.services.repositories.AggregateRepository

trait AggregateRoutes extends RouteUtils with AggregateFormatting {

  def aggregateDriver: AggregateDriverV1
  def aggregateRepo: AggregateRepository

  def aggregateRoutes: Route = authenticateOAuth2(realm, authenticator) { jwt =>
    addAggregate(jwt) ~
      addChildAggregate(jwt) ~
      editAggregate(jwt) ~
      deleteAggregate(jwt) ~
      moveAggregate(jwt)
  }

  private def addAggregate(jwt: String): Route = postPath("api" / "v1" / "aggregates") {
    entity(as[Aggregate]) { agg => completeFrom(aggregateDriver.addAggregate(None, agg, jwt)) }
  }

  private def addChildAggregate(jwt: String): Route = postPath("api" / "v1" / "aggregates" / Segment) { id =>
    entity(as[Aggregate]) { agg => completeFrom(aggregateDriver.addAggregate(Some(id), agg, jwt)) }
  }

  private def editAggregate(jwt: String): Route = putPath("api" / "v1" / "aggregates" / Segment) { id =>
    entity(as[Aggregate]) { agg => completeFrom(aggregateDriver.editAggregate(id, agg, jwt)) }
  }

  private def deleteAggregate(jwt: String): Route = deletePath("api" / "v1" / "aggregates" / Segment) { id =>
    completeFrom(aggregateDriver.deleteAggregate(id, jwt))
  }

  private def moveAggregate(jwt: String): Route =
    postPath("api" / "v1" / "aggregates" / Segment / "move" / Segment) { (id, to) =>
      completeFrom(aggregateDriver.moveAggregate(id, to, jwt))
    }

}

object AggregateRoutes {
  val aggregatePath = "/api/v1/aggregates"
}