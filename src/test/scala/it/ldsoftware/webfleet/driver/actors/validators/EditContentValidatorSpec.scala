package it.ldsoftware.webfleet.driver.actors.validators

import java.time.ZonedDateTime

import it.ldsoftware.webfleet.driver.actors.model._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class EditContentValidatorSpec extends AnyWordSpec with Matchers {

  val subject = new EditContentValidator()

  "validate" should {
    "return no errors" in {
      val form = baseForm
      val parent = okParent

      subject.validate(form, parent) shouldBe List()
    }

    "return an error if trying to insert a calendar and the content is not an event" in {
      val form = baseForm.copy(event =
        Some(WebCalendar(ZonedDateTime.now(), ZonedDateTime.now(), "here", (1, 1)))
      )
      val parent = okParent

      subject.validate(form, parent) shouldBe List(
        ValidationError(
          "event",
          "Cannot insert an event in a non-calendar content",
          "content.notCalendar"
        )
      )
    }

    "return an error if the event is invalid" in {
      val first = ZonedDateTime.now()
      val next = first.plusHours(1)
      val form = baseForm.copy(
        event = Some(WebCalendar(next, first, "here", (1, 1)))
      )
      val parent = okParent.copy(webType = Calendar)

      subject.validate(form, parent) shouldBe List(
        ValidationError("event.start", "Start date cannot be after end date", "date.future")
      )
    }
  }

  private def baseForm: EditingForm = EditingForm(
    "title",
    "description",
    "text",
    "theme",
    "icon",
    None,
    Published
  )

  private def okParent: WebContent = WebContent(
    "parent",
    "/parent",
    Folder,
    "desc",
    "txt",
    "t",
    "i",
    None,
    Published,
    "author",
    None,
    None,
    Map()
  )
}
