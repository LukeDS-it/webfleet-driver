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

    "return an Existing stub when created without explicit indication" in {
      val form = CreateForm(
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
        Stub,
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

    "return a new Existing state with changed values when an Updated event is processed" in {
      val now = ZonedDateTime.now()
      val old = WebContent(
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

      val form = UpdateForm(
        title = Some("new title"),
        description = Some("new description"),
        text = Some("new text"),
        theme = Some("new theme"),
        icon = Some("new icon")
      )

      val expected = WebContent(
        "new title",
        "/parent/child",
        Page,
        "new description",
        "new text",
        "new theme",
        "new icon",
        None,
        Published,
        "name",
        Some(now),
        Some(now),
        Map()
      )

      val user = User("user", Set(), None)

      Existing(old).process(Updated(form, user, now.plusHours(1))) shouldBe Existing(expected)
    }

    "return a published state when an Updated event is give by an user with permissions" in {
      val now = ZonedDateTime.now()
      val old = WebContent(
        "title",
        "/parent/child",
        Page,
        "description",
        "text",
        "theme",
        "icon",
        None,
        Stub,
        "name",
        Some(now),
        None,
        Map()
      )

      val form = UpdateForm(status = Some(Published))

      val editTime = now.plusHours(1)

      val expected = WebContent(
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
        Some(editTime),
        Map()
      )

      val user = User("user", Set(), None)

      Existing(old).process(Updated(form, user, editTime)) shouldBe Existing(expected)
    }

    "return an empty state when a Deleted event is processed" in {
      val now = ZonedDateTime.now()
      val old = WebContent(
        "title",
        "/parent/child",
        Page,
        "description",
        "text",
        "theme",
        "icon",
        None,
        Stub,
        "name",
        Some(now),
        None,
        Map()
      )

      val user = User("name", Set(), None)

      Existing(old).process(Deleted(user, now.plusHours(1))) shouldBe NonExisting("/parent/child")
    }

    "return a new Existing state with a new added child when a ChildAdded event is processed" in {
      val now = ZonedDateTime.now()
      val old = WebContent(
        "title",
        "/parent/child",
        Page,
        "description",
        "text",
        "theme",
        "icon",
        None,
        Stub,
        "name",
        Some(now),
        None,
        Map()
      )

      val child = ContentChild("/parent/child/childer", "child", "child desc", Page)

      Existing(old).process(ChildAdded(child)) shouldBe Existing(
        old.copy(children = Map(child.path -> child))
      )
    }

    "return a new Existing state with an edited child when a ChildUpdated event is processed" in {
      val now = ZonedDateTime.now()
      val child = ContentChild("/parent/child/childer", "child", "child desc", Page)
      val old = WebContent(
        "title",
        "/parent/child",
        Page,
        "description",
        "text",
        "theme",
        "icon",
        None,
        Stub,
        "name",
        Some(now),
        None,
        Map(child.path -> child)
      )

      val nChild = ContentChild("/parent/child/childer", "New Title", "New desc", Page)

      Existing(old).process(ChildUpdated(nChild)) shouldBe Existing(
        old.copy(children = Map(child.path -> nChild))
      )
    }

    "return a new Existing state without a child when a ChildRemoved event is processed" in {
      val now = ZonedDateTime.now()
      val child = ContentChild("/parent/child/childer", "child", "child desc", Page)
      val old = WebContent(
        "title",
        "/parent/child",
        Page,
        "description",
        "text",
        "theme",
        "icon",
        None,
        Stub,
        "name",
        Some(now),
        None,
        Map(child.path -> child)
      )

      Existing(old).process(ChildRemoved(child.path)) shouldBe Existing(old.copy(children = Map()))
    }

    "throw an exception when processing an unprocessable event" in {
      val now = ZonedDateTime.now()
      val child = ContentChild("/parent/child/childer", "child", "child desc", Page)
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
      val old = WebContent(
        "title",
        "/parent/child",
        Page,
        "description",
        "text",
        "theme",
        "icon",
        None,
        Stub,
        "name",
        Some(now),
        None,
        Map(child.path -> child)
      )

      an[IllegalStateException] should be thrownBy Existing(old).process(Created(form, user, now))
    }
  }

}
