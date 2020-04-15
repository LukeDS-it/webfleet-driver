package it.ldsoftware.webfleet.driver.actors.validators

import it.ldsoftware.webfleet.driver.actors.model._

class EditContentValidator {

  type Validator = (EditingForm, WebContent) => Option[ValidationError]

  def validate(form: EditingForm, current: WebContent): List[ValidationError] =
    List(typeValidator, eventDateValidation).flatMap(v => v(form, current))

  private val typeValidator: Validator = (form, current) =>
    current.webType match {
      case Calendar => None
      case _ =>
        if (form.event.isDefined)
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

  private val eventDateValidation: Validator = (form, _) =>
    form.event match {
      case Some(value) => new EventValidator().validate(value)
      case None => None
    }

}
