package it.ldsoftware.webfleet.driver.actors.model

case class CreateForm(
    title: String,
    path: String,
    webType: WebType,
    description: String,
    text: String,
    theme: String,
    icon: String,
    contentStatus: Option[ContentStatus],
    event: Option[WebCalendar]
) {
  def toChild: ContentChild = ContentChild(path, title, description, webType)

  def validationErrors: List[ValidationError] = List(
    pathValidationError, typeValidationError, event.flatMap(_.validationError)
  ).flatten

  private def pathValidationError: Option[ValidationError] =
    if (!path.matches("""^[\w\-]*$"""))
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
