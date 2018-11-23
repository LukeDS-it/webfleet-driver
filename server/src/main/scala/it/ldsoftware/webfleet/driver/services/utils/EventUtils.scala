package it.ldsoftware.webfleet.driver.services.utils

import it.ldsoftware.webfleet.api.v1.events.AggregateEvent
import it.ldsoftware.webfleet.api.v1.events.AggregateEvent._
import it.ldsoftware.webfleet.driver.routes.utils.AggregateFormatting
import spray.json._

object EventUtils extends AggregateFormatting {

  implicit val AggregateEventFormat: JsonFormat[AggregateEvent] = jsonFormat2(AggregateEvent.apply)

  implicit def aggregateEventTypeFormat: RootJsonFormat[AggregateEventType] =
    new RootJsonFormat[AggregateEventType] {
      override def write(obj: AggregateEventType): JsValue = JsString(obj.value)

      override def read(json: JsValue): AggregateEventType = json match {
        case JsString(txt) => AggregateEventType(txt)
        case _ => throw DeserializationException(s"Can'tcreate AggregateEventTypeFormat from $json")
      }
    }

  implicit class JsonEventSyntax[T](kafkaEvent: T) {
    def toJsonString(implicit format: JsonWriter[T]): String = kafkaEvent.toJson.compactPrint
  }

}
