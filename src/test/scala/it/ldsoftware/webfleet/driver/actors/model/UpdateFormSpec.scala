package it.ldsoftware.webfleet.driver.actors.model

import java.time.ZonedDateTime

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class UpdateFormSpec extends AnyWordSpec with Matchers {

  private val now = ZonedDateTime.now()
  private val base = WebContent(
    "title",
    "/parent/child",
    Page,
    "description",
    "text",
    "theme",
    "icon",
    None,
    Published,
    "name",
    Some(now),
    Some(now),
    Map()
  )

  "The validation function" should {

    "return no error if the form is correct" in {
      UpdateForm(title = Some("New title")).validationErrors(base) shouldBe List()
    }

    "return an error if there is an event in a non calendar content" in {
      UpdateForm(event = Some(WebCalendar(now, now.plusHours(1), "Venice", (45.438759, 12.327145))))
        .validationErrors(base) shouldBe List(
        ValidationError(
          "event",
          "Cannot insert an event in a non-calendar content",
          "content.notCalendar"
        )
      )
    }

    "return an error if the event has an error" in {
      UpdateForm(event = Some(WebCalendar(now.plusHours(1), now, "Venice", (45.438759, 12.327145))))
        .validationErrors(base.copy(webType = Calendar)) shouldBe List(
        ValidationError("event.start", "Start date cannot be after end date", "date.future")
      )
    }

  }

}
