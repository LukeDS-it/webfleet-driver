package it.ldsoftware.webfleet.driver.services.v1

import com.typesafe.scalalogging.LazyLogging
import it.ldsoftware.webfleet.api.v1.events.AggregateEvent
import it.ldsoftware.webfleet.api.v1.events.AggregateEvent._
import it.ldsoftware.webfleet.api.v1.model._
import it.ldsoftware.webfleet.api.v1.service.AggregateDriverV1
import it.ldsoftware.webfleet.driver.services.repositories.AggregateRepository
import it.ldsoftware.webfleet.driver.services.utils.AuthenticationUtils._
import it.ldsoftware.webfleet.driver.services.utils.EventUtils._
import it.ldsoftware.webfleet.driver.services.utils.{AuthenticationUtils, ValidationUtils}
import it.ldsoftware.webfleet.driver.services.v1.AggregateService._
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

import scala.util.{Failure, Success, Try}

class AggregateService(kafka: KafkaProducer[String, String], repo: AggregateRepository)
  extends AggregateDriverV1
    with AuthenticationUtils
    with ValidationUtils
    with LazyLogging {

  override def addAggregate(parentAggregate: Option[String], aggregate: Aggregate, jwt: String): DriverResult =
    authorize(jwt, RoleAddAggregate) { _ =>
      validate(newAggregateValidator(parentAggregate, aggregate)) {
        Try {
          repo.addAggregate(parentAggregate, aggregate)
        } map { _ =>
          val evt = AggregateEvent(AddAggregate, Some(aggregate)).toJsonString
          val record = new ProducerRecord[String, String](TopicName, aggregate.name.get, evt)
          kafka.send(record).get()
          evt
        } match {
          case Success(evt) =>
            logger.info(s"Sent event $evt")
            Created(aggregate.name.get)
          case Failure(exception) =>
            logger.error("Error while adding aggregate", exception)
            ServerError(exception.getMessage)
        }
      }
    }

  override def editAggregate(name: String, aggregate: Aggregate, jwt: String): DriverResult =
    authorize(jwt, RoleEditAggregate) { _ =>
      if (!repo.existsByName(name)) addAggregate(None, aggregate, jwt)
      else
        validate(editedAggregateValidator(name, aggregate)) {
          val old = repo.getAggregate(name).get
          val mix = Aggregate(
            aggregate.name.orElse(old.name),
            aggregate.description.orElse(old.description),
            aggregate.text.orElse(old.text)
          )

          Try {
            repo.updateAggregate(name, mix)
          } map { _ =>
            val evt = AggregateEvent(EditAggregate, Some(mix)).toJsonString
            val record = new ProducerRecord[String, String](TopicName, old.name.get, evt)
            kafka.send(record).get()
            evt
          } match {
            case Success(evt) =>
              logger.info(s"Sent event $evt")
              NoContent
            case Failure(exception) =>
              logger.error("Error while editing aggregate", exception)
              ServerError(exception.getMessage)
          }
        }
    }

  override def deleteAggregate(name: String, jwt: String): DriverResult =
    authorize(jwt, RoleDeleteAggregate) { _ =>
      Try {
        repo.deleteAggregate(name)
      } map { _ =>
        val evt = AggregateEvent(DeleteAggregate, Some(Aggregate(Some(name), None, None))).toJsonString
        val record = new ProducerRecord[String, String](TopicName, name, evt)
        kafka.send(record).get()
        evt
      } match {
        case Success(evt) =>
          logger.info(s"Sent event $evt")
          NoContent
        case Failure(exception) =>
          logger.error("Error while deleting aggregate", exception)
          ServerError(exception.getMessage)
      }
    }

  override def moveAggregate(name: String, destination: String, jwt: String): DriverResult =
    authorize(jwt, RoleMoveAggregate) { _ =>
      validate(moveAggregateValidator(name, destination)) {
        Try {
          repo.moveAggregate(name, destination)
        } map { _ =>
          val evt = AggregateEvent(MoveAggregate, Some(Aggregate(Some(destination), None, None))).toJsonString
          val record = new ProducerRecord[String, String](TopicName, name, evt)
          kafka.send(record).get()
          evt
        } match {
          case Success(evt) =>
            logger.info(s"Sent event $evt")
            NoContent
          case Failure(exception) =>
            logger.error("Error while moving aggregate", exception)
            ServerError(exception.getMessage)
        }
      }
    }

  private def newAggregateValidator(parent: Option[String], agg: Aggregate): Set[FieldError] = {
    var arr = Set[FieldError]()

    if (agg.name.isEmpty)
      arr = arr + FieldError("name", "Aggregate name cannot be empty")
    else if (repo.existsByName(agg.name.get))
      arr = arr + FieldError("name", "Aggregate with same name already exists")

    if (agg.text.isEmpty)
      arr = arr + FieldError("text", "Aggregate text cannot be empty")

    for (p <- parent) yield {
      if (!repo.existsByName(p))
        arr = arr + FieldError("parent", "Specified parent does not exist")
    }

    arr
  }

  private def editedAggregateValidator(name: String, agg: Aggregate): Set[FieldError] = {
    var arr = Set[FieldError]()

    if (agg.name.exists(s => repo.existsByName(s) && s != name))
      arr = arr + FieldError("name", "Aggregate with same name already exists")
    if (agg.text.exists(_.trim.isEmpty))
      arr = arr + FieldError("text", "Aggregate text cannot be empty")

    arr
  }

  private def moveAggregateValidator(name: String, dest: String): Set[FieldError] = if (repo.existsByName(dest))
    Set()
  else
    Set(FieldError("destination", "Destination aggregate does not exist"))
}

object AggregateService {
  val TopicName = "aggregates"
}