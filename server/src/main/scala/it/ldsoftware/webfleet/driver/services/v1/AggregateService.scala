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
        repo.addAggregate(parentAggregate, aggregate)
        // TODO send event to Kafka
        Created("1")
      }
    }

  override def editAggregate(name: String, aggregate: Aggregate, jwt: String): DriverResult =
    authorize(jwt, RoleEditAggregate) { _ =>
      if (!repo.existsByName(name)) addAggregate(None, aggregate, jwt)
      else
        validate(editedAggregateValidator(aggregate)) {
          val old = repo.getAggregate(name).get
          val mix = Aggregate(aggregate.name.orElse(old.name), aggregate.description.orElse(old.description), aggregate.text.orElse(old.text))
          repo.updateAggregate(name, mix)
          // TODO send event to Kafka
          NoContent
        }
    }

  override def deleteAggregate(name: String, jwt: String): DriverResult =
    authorize(jwt, RoleDeleteAggregate) { _ =>
      repo.deleteAggregate(name)
      // TODO send event to Kafka
      NoContent
    }

  override def moveAggregate(name: String, destination: String, jwt: String): DriverResult =
    authorize(jwt, RoleMoveAggregate) { _ =>
      validate(moveAggregateValidator(name, destination)) {
        repo.moveAggregate(name, destination)
        // TODO send event to Kafka
        NoContent
      }
    }

  private def newAggregateValidator(parent: Option[String], agg: Aggregate): Array[FieldError] = {
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

  private def editedAggregateValidator(agg: Aggregate): Array[FieldError] = {
    var arr = Array[FieldError]()

    if (agg.name.isDefined && repo.existsByName(agg.name.get))
      arr = arr :+ FieldError("name", "Aggregate with same name already exists")
    if (agg.text.isEmpty)
      arr = arr :+ FieldError("text", "Aggregate text cannot be empty")

    arr
  }

  private def moveAggregateValidator(name: String, dest: String): Array[FieldError] = if (repo.existsByName(dest))
    Array()
  else
    Array(FieldError("destination", "Destination aggregate does not exist"))
}
