package it.ldsoftware.webfleet.driver.routes

import akka.http.scaladsl.server.Route
import it.ldsoftware.webfleet.api.v1.auth.Principal
import it.ldsoftware.webfleet.api.v1.model.Aggregate
import it.ldsoftware.webfleet.api.v1.service.AggregateDriverV1
import it.ldsoftware.webfleet.driver.routes.utils.{AggregateFormatting, RouteUtils}
import it.ldsoftware.webfleet.driver.services.repositories.AggregateRepository

trait AggregateRoutes extends RouteUtils with AggregateFormatting {

  def aggregateDriver: AggregateDriverV1
  def aggregateRepo: AggregateRepository

  def aggregateRoutes: Route = authenticateOAuth2(realm, authenticator) { principal =>
    addAggregate(principal) ~
      addChildAggregate(principal) ~
      editAggregate(principal) ~
      deleteAggregate(principal) ~
      moveAggregate(principal)
  }

  private def addAggregate(principal: Principal): Route = postPath("api" / "v1" / "aggregates") {
    entity(as[Aggregate]) { agg => completeFrom(aggregateDriver.addAggregate(None, agg, principal)) }
  }

  private def addChildAggregate(principal: Principal): Route = postPath("api" / "v1" / "aggregates" / Segment) { id =>
    entity(as[Aggregate]) { agg => completeFrom(aggregateDriver.addAggregate(Some(id), agg, principal)) }
  }

  private def editAggregate(principal: Principal): Route = putPath("api" / "v1" / "aggregates" / Segment) { id =>
    entity(as[Aggregate]) { agg => completeFrom(aggregateDriver.editAggregate(id, agg, principal)) }
  }

  private def deleteAggregate(principal: Principal): Route = deletePath("api" / "v1" / "aggregates" / Segment) { id =>
    completeFrom(aggregateDriver.deleteAggregate(id, principal))
  }

  private def moveAggregate(principal: Principal): Route =
    postPath("api" / "v1" / "aggregates" / Segment / "move" / Segment) { (id, to) =>
      completeFrom(aggregateDriver.moveAggregate(id, to, principal))
    }

}

object AggregateRoutes {
  val aggregatePath = "/api/v1/aggregates"
}