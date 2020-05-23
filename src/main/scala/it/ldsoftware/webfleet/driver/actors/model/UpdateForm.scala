package it.ldsoftware.webfleet.driver.actors.model

import it.ldsoftware.webfleet.driver.actors.serialization.CborSerializable

case class UpdateForm(
    title: Option[String] = None,
    description: Option[String] = None,
    text: Option[String] = None,
    theme: Option[String] = None,
    icon: Option[String] = None,
    event: Option[WebCalendar] = None,
    status: Option[ContentStatus] = None
) extends CborSerializable {
  def validationErrors(base: WebContent): List[ValidationError] =
    List(
      typeValidationError(base),
      event.flatMap(_.validationError)
    ).flatten

  private def typeValidationError(base: WebContent): Option[ValidationError] =
    base.webType match {
      case Calendar => None
      case _ =>
        if (event.isDefined)
          Some(
            ValidationError(
              "event",
              "Cannot insert an event in a non-calendar content",
              "content.notCalendar"
            )
          )
        else
          None
    }
}
