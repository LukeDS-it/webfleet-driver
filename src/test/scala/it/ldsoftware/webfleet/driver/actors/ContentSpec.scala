package it.ldsoftware.webfleet.driver.actors

import java.time.ZonedDateTime

import it.ldsoftware.webfleet.driver.actors.Content._
import it.ldsoftware.webfleet.driver.actors.model._
import it.ldsoftware.webfleet.driver.security.{Permissions, User}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ContentSpec extends AnyWordSpec with Matchers {

  "The empty state" should {
    "return an Existing state when a Created event is processed" in {
      val form = CreateForm(
        "title",
        "/parent/child",
        Page,
        "description",
        "text",
        "theme",
        "icon",
        Some(Published),
        None
      )
      val user = User("name", Set(Permissions.Contents.Publish), None)
      val now = ZonedDateTime.now()
      val webContent = WebContent(
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

      NonExisting("/").process(Created(form, user, now)) shouldBe Existing(webContent)
    }

    "return an Existing non published state when Created was issued by a non authorized user" in {
      val form = CreateForm(
        "title",
        "/parent/child",
        Page,
        "description",
        "text",
        "theme",
        "icon",
        Some(Published),
        None
      )
      val user = User("name", Set(), None)
      val now = ZonedDateTime.now()
      val webContent = WebContent(
        "title",
        "/parent/child",
        Page,
        "description",
        "text",
        "theme",
        "icon",
        None,
        Review,
        "name",
        Some(now),
        None,
        Map()
      )

      NonExisting("/").process(Created(form, user, now)) shouldBe Existing(webContent)
    }

    "throw an exception when any other event is processed" in {
      val user = User("name", Set(), None)
      val now = ZonedDateTime.now()

      an[IllegalStateException] should be thrownBy NonExisting("/").process(Deleted(user, now))
    }
  }

  "The existing state" should {
    "return a new Existing state with changed values when an Updated event is processed" in {}

    "return a published state when an Updated event is give by an user with permissions" in {}

    "do not change publish date if two Updated events with Publishing are given in sequence" in {}

    "return an empty state when a Deleted event is processed" in {}

    "return a new Existing state with a new added child when a ChildAdded event is processed" in {}

    "return a new Existing state with an edited child when a ChildUpdated event is processed" in {}

    "return a new Existing state without a child when a ChildRemoved event is processed" in {}
  }

}
