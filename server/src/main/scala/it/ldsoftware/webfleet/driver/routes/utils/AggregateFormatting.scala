package it.ldsoftware.webfleet.driver.routes.utils

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import it.ldsoftware.webfleet.api.v1.model.Aggregate
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

trait AggregateFormatting extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val AggregateFormat: RootJsonFormat[Aggregate] = jsonFormat3(Aggregate)

}
