package it.ldsoftware.webfleet.driver.actors.model

import java.time.ZonedDateTime

import it.ldsoftware.webfleet.commons.service.model.ValidationError
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class CreateFormSpec extends AnyWordSpec with Matchers {

  val path = "path"

  "The validation function" should {

    "return no errors when the form is correct" in {
      CreateForm(
        "title",
        path,
        Folder,
        "description",
        "text",
        "theme",
        "icon",
        None,
        None
      ).validationErrors(path) shouldBe List()
    }

    "return no errors when the path has forward slashes" in {
      CreateForm(
        "title",
        "path/with/slashes",
        Folder,
        "description",
        "text",
        "theme",
        "icon",
        None,
        None
      ).validationErrors("path/with/slashes") shouldBe List()
    }

    "return an error if the path is invalid" in {
      CreateForm(
        "title",
        "path with spaces",
        Folder,
        "description",
        "text",
        "theme",
        "icon",
        None,
        None
      ).validationErrors("path with spaces") shouldBe List(
        ValidationError("path", "Path cannot contain symbols except - and _", "path.pattern")
      )
    }

    "return an error if there is no event in a calendar content" in {
      CreateForm(
        "title",
        path,
        Calendar,
        "description",
        "text",
        "theme",
        "icon",
        None,
        None
      ).validationErrors(path) shouldBe List(
        ValidationError("event", "Event is missing", "event.notEmpty")
      )
    }

    "return an error if there is an event in a non calendar content" in {
      val now = ZonedDateTime.now()
      CreateForm(
        "title",
        path,
        Folder,
        "description",
        "text",
        "theme",
        "icon",
        None,
        Some(WebCalendar(now, now.plusHours(1), "Venice", (45.438759, 12.327145)))
      ).validationErrors(path) shouldBe List(
        ValidationError(
          "event",
          "Event is present but content type is not Calendar",
          "content.notCalendar"
        )
      )
    }

    "return an error if the event has some error" in {
      val now = ZonedDateTime.now()
      CreateForm(
        "title",
        path,
        Calendar,
        "description",
        "text",
        "theme",
        "icon",
        None,
        Some(WebCalendar(now.plusHours(1), now, "Venice", (45.438759, 12.327145)))
      ).validationErrors(path) shouldBe List(
        ValidationError("event.start", "Start date cannot be after end date", "date.future")
      )
    }

    "return an error if the path mismatches" in {
      CreateForm(
        "title",
        "/another/path",
        Folder,
        "description",
        "text",
        "theme",
        "icon",
        None,
        None
      ).validationErrors(path) shouldBe List(
        ValidationError("path", "Path is not the same as http path", "path.location")
      )
    }
  }

  "the toChild function" should {
    "return a ContentChild" in {
      CreateForm(
        "title",
        path,
        Calendar,
        "description",
        "text",
        "theme",
        "icon",
        None,
        None
      ).toChild shouldBe ContentChild("path", "title", "description", Calendar)
    }
  }

}
