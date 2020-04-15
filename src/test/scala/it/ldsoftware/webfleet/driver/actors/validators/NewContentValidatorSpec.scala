package it.ldsoftware.webfleet.driver.actors.validators

import java.time.ZonedDateTime

import it.ldsoftware.webfleet.driver.actors.model._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class NewContentValidatorSpec extends AnyWordSpec with Matchers {

  val subject = new NewContentValidator()

  "validate" should {
    "return no errors" in {
      val form = baseForm
      val parent = okParent

      subject.validate(form, parent) shouldBe List()
    }

    "return an error if a child is already present" in {
      val form = baseForm
      val parent = okParent.copy(
        children = Map("/parent/child" -> ContentChild("/parent/child", "title", "d", Page))
      )

      subject.validate(form, parent) shouldBe List(
        ValidationError("path", "Selected path already exists", "path.duplicate")
      )
    }

    "return an error if parent is not a folder" in {
      val form = baseForm
      val parent = okParent.copy(webType = Page)

      subject.validate(form, parent) shouldBe List(
        ValidationError("parent", "Parent is not a folder", "parent.notFolder")
      )
    }

    "return an error if a calendar is present but the content is not an event" in {
      val form = baseForm.copy(event =
        Some(WebCalendar(ZonedDateTime.now(), ZonedDateTime.now(), "here", (1, 1)))
      )
      val parent = okParent

      subject.validate(form, parent) shouldBe List(
        ValidationError(
          "event",
          "Event is present but content type is not Calendar",
          "content.notCalendar"
        )
      )
    }

    "return an error if no calendar is present on an event" in {
      val form = baseForm.copy(webType = Calendar)
      val parent = okParent

      subject.validate(form, parent) shouldBe List(
        ValidationError("event", "Event is missing", "event.notEmpty")
      )
    }

    "return an error if the event is invalid" in {
      val first = ZonedDateTime.now()
      val next = first.plusHours(1)
      val form = baseForm.copy(
        webType = Calendar,
        event = Some(WebCalendar(next, first, "here", (1, 1)))
      )
      val parent = okParent

      subject.validate(form, parent) shouldBe List(
        ValidationError("event.start", "Start date cannot be after end date", "date.future")
      )
    }
  }

  private def baseForm: CreationForm = CreationForm(
    "title",
    "/parent/child",
    Page,
    "description",
    "text",
    "theme",
    "icon",
    None,
    None
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
