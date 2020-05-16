package it.ldsoftware.webfleet.driver.actors.validators

import it.ldsoftware.webfleet.driver.actors.model._

class NewContentValidator {

  type Validator = (CreationForm, WebContent) => Option[ValidationError]

  def validate(form: CreationForm, parent: WebContent): List[ValidationError] =
    List(parentValidator, pathValidator, typeValidator, eventDateValidation)
      .flatMap(v => v(form, parent))

  private val pathValidator: Validator = (form, parent) =>
    if (parent.children.keys.exists(_ == form.path))
      Some(ValidationError("path", "Selected path already exists", "path.duplicate"))
    else
      None

  private val parentValidator: Validator = (_, parent) =>
    parent.webType match {
      case Folder => None
      case _      => Some(ValidationError("parent", "Parent is not a folder", "parent.notFolder"))
    }

  private val typeValidator: Validator = (form, _) =>
    form.webType match {
      case Calendar =>
        if (form.event.isDefined) None
        else Some(ValidationError("event", "Event is missing", "event.notEmpty"))
      case _ =>
        if (form.event.isDefined)
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

  private val eventDateValidation: Validator = (form, _) =>
    form.event match {
      case Some(value) => new EventValidator().validate(value)
      case None        => None
    }

}
