package it.ldsoftware.webfleet.driver.actors.model

import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import com.fasterxml.jackson.databind.annotation.{JsonDeserialize, JsonSerialize}
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.databind.{DeserializationContext, SerializerProvider}
import io.circe.{Decoder, Encoder}
import it.ldsoftware.webfleet.driver.actors.serialization.CborSerializable

@JsonSerialize(using = classOf[WebTypeJsonSerializer])
@JsonDeserialize(using = classOf[WebTypeJsonDeserializer])
sealed trait WebType extends CborSerializable

case object Page extends WebType

case object Folder extends WebType

case object Calendar extends WebType

class WebTypeJsonSerializer extends StdSerializer[WebType](classOf[WebType]) {
  override def serialize(value: WebType, gen: JsonGenerator, provider: SerializerProvider): Unit = {
    val strVal = value match {
      case Page     => "p"
      case Folder   => "f"
      case Calendar => "c"
    }
    gen.writeString(strVal)
  }
}

class WebTypeJsonDeserializer extends StdDeserializer[WebType](classOf[WebType]) {
  override def deserialize(p: JsonParser, ctx: DeserializationContext): WebType =
    p.getText match {
      case "p" => Page
      case "f" => Folder
      case "c" => Calendar
    }
}

object WebType {
  implicit val decodeWebType: Decoder[WebType] = Decoder[String].emap { s =>
    s.toLowerCase match {
      case "page"     => Right(Page)
      case "folder"   => Right(Folder)
      case "calendar" => Right(Calendar)
      case other      => Left(s"Invalid content status: $other")
    }
  }

  implicit val encodeWebType: Encoder[WebType] = Encoder[String].contramap {
    case Page     => "page"
    case Folder   => "folder"
    case Calendar => "calendar"
  }
}
