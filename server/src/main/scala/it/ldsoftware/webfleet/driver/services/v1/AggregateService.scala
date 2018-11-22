package it.ldsoftware.webfleet.driver.services.v1

import it.ldsoftware.webfleet.api.v1.model._
import it.ldsoftware.webfleet.api.v1.service.AggregateDriverV1
import it.ldsoftware.webfleet.driver.services.KafkaService
import it.ldsoftware.webfleet.driver.services.repositories.AggregateRepository
import it.ldsoftware.webfleet.driver.services.utils.AuthenticationUtils._
import it.ldsoftware.webfleet.driver.services.utils.{AuthenticationUtils, ValidationUtils}

class AggregateService(kafka: KafkaService, repo: AggregateRepository)
  extends AggregateDriverV1 with AuthenticationUtils with ValidationUtils {

  override def addAggregate(parentAggregate: Option[String], aggregate: Aggregate, jwt: String): DriverResult =
    authorize(jwt, RoleAddAggregate) { _ =>
      validate(newAggregateValidator(parentAggregate, aggregate)) {
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
      validate(moveAggregateValidator(name, destination)) {
        NoContent
      }
    }

  def newAggregateValidator(parent: Option[String], agg: Aggregate): Array[FieldError] = {
    var arr = Array[FieldError]()

    if (agg.name.isEmpty)
      arr = arr :+ FieldError("name", "Aggregate name cannot be empty")
    if (repo.existsByName(agg.name.get))
      arr = arr :+ FieldError("name", "Aggregate with same name already exists")
    if (agg.text.isEmpty)
      arr = arr :+ FieldError("text", "Aggregate text cannot be empty")

    for (p <- parent) yield {
      if (repo.existsByName(p))
        arr = arr :+ FieldError("parent", "Specified parent does not exist")
    }

    arr
  }

  def editedAggregateValidator(agg: Aggregate): Array[FieldError] = if (agg.text.isEmpty)
    Array(FieldError("text", "Aggregate text cannot be empty"))
  else
    Array()

  def moveAggregateValidator(name: String, dest: String): Array[FieldError] = if (repo.existsByName(dest))
    Array()
  else
    Array(FieldError("destination", "Destination aggregate does not exist"))
}
