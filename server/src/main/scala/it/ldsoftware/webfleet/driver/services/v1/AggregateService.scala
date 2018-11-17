package it.ldsoftware.webfleet.driver.services.v1

import it.ldsoftware.webfleet.api.v1.model._
import it.ldsoftware.webfleet.api.v1.service.AggregateDriverV1
import it.ldsoftware.webfleet.driver.services.KafkaService

class AggregateService(kafka: KafkaService) extends AggregateDriverV1 {
  override def addAggregate(parentAggregate: Option[String], aggregate: Aggregate, jwt: String): DriverResult = Created("1")

  override def editAggregate(name: String, aggregate: Aggregate, jwt: String): DriverResult = NoContent

  override def deleteAggregate(name: String, jwt: String): DriverResult = NoContent

  override def moveAggregate(name: String, destination: String, jwt: String): DriverResult = NoContent
}
