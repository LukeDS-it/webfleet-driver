package it.ldsoftware.webfleet.driver.services.v1

import it.ldsoftware.webfleet.api.v1.model._
import it.ldsoftware.webfleet.api.v1.service.AggregateDriverV1
import it.ldsoftware.webfleet.driver.services.KafkaService
import it.ldsoftware.webfleet.driver.services.utils.{AuthenticationUtils, ValidationUtils}
import AuthenticationUtils._

class AggregateService(kafka: KafkaService) extends AggregateDriverV1 with AuthenticationUtils with ValidationUtils {

  override def addAggregate(parentAggregate: Option[String], aggregate: Aggregate, jwt: String): DriverResult =
    authorize(jwt, RoleAddAggregate) { _ =>
      validate(newAggregateValidator(aggregate)) {
        Created("1")
      }
    }

  override def editAggregate(name: String, aggregate: Aggregate, jwt: String): DriverResult =
    authorize(jwt, RoleEditAggregate) { principal =>
      validate(editedAggregateValidator(aggregate)) {
        NoContent
      }
    }

  override def deleteAggregate(name: String, jwt: String): DriverResult =
    authorize(jwt, RoleDeleteAggregate) { principal =>
      NoContent
    }

  override def moveAggregate(name: String, destination: String, jwt: String): DriverResult =
    authorize(jwt, RoleMoveAggregate) { principal =>
      validate(moveAggregateValidator(destination)) {
        NoContent
      }
    }

  def newAggregateValidator(agg: Aggregate): List[FieldError] = {
    List()
  }

  def editedAggregateValidator(agg: Aggregate): List[FieldError] = {
    List()
  }

  def moveAggregateValidator(dest: String): List[FieldError] = List()
}
