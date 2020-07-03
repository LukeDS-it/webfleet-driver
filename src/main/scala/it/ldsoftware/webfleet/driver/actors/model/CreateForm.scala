package it.ldsoftware.webfleet.driver.actors.model

import it.ldsoftware.webfleet.commons.service.model.ValidationError
import it.ldsoftware.webfleet.driver.actors.serialization.CborSerializable

case class CreateForm(
    title: String,
    path: String,
    webType: WebType,
    description: String,
    text: String,
    theme: String = "default",
    icon: String = "default.png",
    contentStatus: Option[ContentStatus] = None,
    event: Option[WebCalendar] = None
) extends CborSerializable {
  def toChild: ContentChild = ContentChild(path, title, description, webType)

  def validationErrors(myPath: String): List[ValidationError] =
    List(
      myPathValidationError(myPath),
      pathValidationError,
      typeValidationError,
      event.flatMap(_.validationError)
    ).flatten

  private def myPathValidationError(myPath: String): Option[ValidationError] =
    if (myPath == path)
      None
    else
      Some(ValidationError("path", "Path is not the same as http path", "path.location"))

  private def pathValidationError: Option[ValidationError] =
    if (!path.matches("""^[\w\-/]*$"""))
      Some(ValidationError("path", "Path cannot contain symbols except - and _", "path.pattern"))
    else
      None

  private def typeValidationError: Option[ValidationError] =
    webType match {
      case Calendar =>
        if (event.isDefined) None
        else Some(ValidationError("event", "Event is missing", "event.notEmpty"))
      case _ =>
        if (event.isDefined)
          Some(
            ValidationError(
              "event",
              "Event is present but content type is not Calendar",
              "content.notCalendar"
            )
          )
        else
          None
    }

}
