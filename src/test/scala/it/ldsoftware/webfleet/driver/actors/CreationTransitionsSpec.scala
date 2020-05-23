package it.ldsoftware.webfleet.driver.actors

import it.ldsoftware.webfleet.driver.actors.Creation._
import it.ldsoftware.webfleet.driver.actors.model.{CreateForm, Folder, ValidationError}
import it.ldsoftware.webfleet.driver.security.User
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class CreationTransitionsSpec extends AnyWordSpec with Matchers {

  val anyForm: CreateForm = CreateForm(
    "title",
    "path",
    Folder,
    "description",
    "text",
    "theme",
    "icon",
    None,
    None
  )

  val anyUser: User = User("name", Set(), None)

  val reason: String = "reason"

  val errs: List[ValidationError] = List(ValidationError("a", "b", "c"))

  "The idle state" should {
    "go to awaiting tree if a started event is processed" in {
      Idle.process(Started(anyForm, anyUser)) shouldBe AwaitingTree(anyForm, anyUser)
    }

    "refuse any other event" in {
      an[IllegalArgumentException] should be thrownBy Idle.process(TreeUpdated)
    }
  }

  "The awaiting tree state" should {
    "go to awaiting content if a tree updated event is processed" in {
      AwaitingTree(anyForm, anyUser).process(TreeUpdated) shouldBe AwaitingContent(anyForm, anyUser)
    }

    "go to failed if a tree failed event is processed" in {
      AwaitingTree(anyForm, anyUser).process(TreeFailed(reason, errs)) shouldBe Failed(reason, errs)
    }

    "refuse any other event" in {
      an[IllegalArgumentException] should be thrownBy AwaitingTree(anyForm, anyUser)
        .process(Started(anyForm, anyUser))
    }
  }

  "The awaiting content state" should {
    "go to completed if a content created event is processed" in {
      AwaitingContent(anyForm, anyUser).process(ContentCreated) shouldBe Completed
    }

    "go to awaiting rollback if a content failed event is processed" in {
      AwaitingContent(anyForm, anyUser)
        .process(ContentFailed(reason, errs)) shouldBe AwaitingRollback(anyForm, reason, errs)
    }

    "refuse any other event" in {
      an[IllegalArgumentException] should be thrownBy AwaitingContent(anyForm, anyUser).process(
        Started(anyForm, anyUser)
      )
    }
  }

  "The awaiting rollback state" should {
    "go to failed if a tree rolled back event is processed" in {
      AwaitingRollback(anyForm, reason, errs).process(TreeRolledBack) shouldBe Failed(reason, errs)
    }

    "refuse any other event" in {
      an[IllegalArgumentException] should be thrownBy AwaitingRollback(anyForm, reason, errs)
        .process(Started(anyForm, anyUser))
    }
  }

  "The completed state" should {
    "have no state transition" in {
      an[IllegalArgumentException] should be thrownBy Completed.process(TreeUpdated)
    }
  }

  "The failed state" should {
    "have no state transition" in {
      an[IllegalArgumentException] should be thrownBy Failed("", Nil).process(TreeUpdated)
    }
  }

}
