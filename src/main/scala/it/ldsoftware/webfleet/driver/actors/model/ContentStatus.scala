package it.ldsoftware.webfleet.driver.actors.model

import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import com.fasterxml.jackson.databind.annotation.{JsonDeserialize, JsonSerialize}
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.databind.{DeserializationContext, SerializerProvider}
import it.ldsoftware.webfleet.driver.actors.serialization.CborSerializable

@JsonSerialize(using = classOf[ContentStatusJsonSerializer])
@JsonDeserialize(using = classOf[ContentStatusJsonDeserializer])
sealed trait ContentStatus extends CborSerializable

case object Stub extends ContentStatus

case object Review extends ContentStatus

case object Rejected extends ContentStatus

case object Published extends ContentStatus

class ContentStatusJsonSerializer extends StdSerializer[ContentStatus](classOf[ContentStatus]) {
  override def serialize(
      value: ContentStatus,
      gen: JsonGenerator,
      provider: SerializerProvider
  ): Unit = {
    val strVal = value match {
      case Stub      => "s"
      case Review    => "w"
      case Rejected  => "j"
      case Published => "p"
    }
    gen.writeString(strVal)
  }
}

class ContentStatusJsonDeserializer extends StdDeserializer[ContentStatus](classOf[ContentStatus]) {
  override def deserialize(p: JsonParser, ctx: DeserializationContext): ContentStatus =
    p.getText match {
      case "s" => Stub
      case "w" => Review
      case "j" => Rejected
      case "p" => Published
    }
}
