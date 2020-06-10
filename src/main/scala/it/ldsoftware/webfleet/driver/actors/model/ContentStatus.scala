package it.ldsoftware.webfleet.driver.actors.model

import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import com.fasterxml.jackson.databind.annotation.{JsonDeserialize, JsonSerialize}
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.databind.{DeserializationContext, SerializerProvider}
import io.circe.{Decoder, Encoder}
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

object ContentStatus {
  implicit val decodeContentStatus: Decoder[ContentStatus] = Decoder[String].emap { s =>
    s.toLowerCase match {
      case "stub"      => Right(Stub)
      case "review"    => Right(Review)
      case "rejected"  => Right(Rejected)
      case "published" => Right(Published)
      case other       => Left(s"Invalid content status: $other")
    }
  }

  implicit val encodeContentStatus: Encoder[ContentStatus] = Encoder[String].contramap {
    case Stub      => "stub"
    case Review    => "review"
    case Rejected  => "rejected"
    case Published => "published"
  }
}
