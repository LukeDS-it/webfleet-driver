package it.ldsoftware.webfleet.api.v1.events

import it.ldsoftware.webfleet.api.v1.events.AggregateEvent.AggregateEventType
import it.ldsoftware.webfleet.api.v1.model.Aggregate

case class AggregateEvent(eventType: AggregateEventType, subject: Option[Aggregate])

object AggregateEvent {

  sealed trait AggregateEventType {
    def value: String
  }

  case object AddAggregate extends AggregateEventType {
    override def value: String = "A"
  }

  case object EditAggregate extends AggregateEventType {
    override def value: String = "E"
  }

  case object MoveAggregate extends AggregateEventType {
    override def value: String = "M"
  }

  case object DeleteAggregate extends AggregateEventType {
    override def value: String = "D"
  }

  object AggregateEventType {
    def apply(txt: String): AggregateEventType = txt match {
      case "A" => AddAggregate
      case "E" => EditAggregate
      case "M" => MoveAggregate
      case "D" => DeleteAggregate
    }
  }

}