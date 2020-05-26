package it.ldsoftware.webfleet.driver.actors

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.ActorRef
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import it.ldsoftware.webfleet.driver.actors.Content._
import it.ldsoftware.webfleet.driver.actors.model._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.wordspec.AnyWordSpecLike

class ContentSpec
    extends ScalaTestWithActorTestKit(
      EventSourcedBehaviorTestKit.config.withFallback(CQRSTestKit.config)
    )
    with CQRSTestKit
    with AnyWordSpecLike
    with BeforeAndAfterEach {

  private val rootTestKit =
    EventSourcedBehaviorTestKit[Command, Event, State](system, Content("/", timeServer))

  override def beforeEach(): Unit = {
    super.beforeEach()
    rootTestKit.clear()
  }

  "An empty content" should {

    "return not found when asked for its state" in {
      val result = rootTestKit.runCommand(Read)
      result.reply shouldBe NotFound("/")
      result.events shouldBe empty
    }

    "return done when creating a content, and save the correct status" in {
      val form = rootForm
      val user = superUser
      val expectedRootContent = getExpectedContent(form, user, Published)

      val result = rootTestKit.runCommand[Response](Create(form, user, _))

      result.reply shouldBe Done
      result.event shouldBe Created(form, user, testTime)
      result.state shouldBe Existing(expectedRootContent)
    }

    "return validation errors when content is invalid" in {
      val form = rootForm.copy(path = "invalid path")
      val user = superUser

      val result = rootTestKit.runCommand[Response](Create(form, user, _))

      val pathLocation =
        ValidationError("path", "Path is not the same as http path", "path.location")

      val pathPattern =
        ValidationError("path", "Path cannot contain symbols except - and _", "path.pattern")

      result.reply shouldBe Invalid(List(pathLocation, pathPattern))
      result.events shouldBe empty
    }

  }

  "An existing content" should {

    "return its contents when asked" in {
      val form = rootForm
      val user = superUser

      rootTestKit.runCommand[Response](Create(form, user, _))
      val result = rootTestKit.runCommand[Response](Read)

      result.reply shouldBe MyContent(getExpectedContent(form, user, Published))
    }

    "return a validation error if trying to create an already existing content" in {
      val form = rootForm
      val user = superUser

      rootTestKit.runCommand[Response](Create(form, user, _))
      val result = rootTestKit.runCommand[Response](Create(form, user, _))

      val alreadyExists = ValidationError("path", s"Content at / already exists", "path.duplicate")

      result.reply shouldBe Invalid(List(alreadyExists))
    }

    "correctly update its contents" in {
      val form = rootForm
      val user = superUser
      val update = UpdateForm(title = Some("New title"))

      rootTestKit.runCommand[Response](Create(form, user, _))

      val result = rootTestKit.runCommand[Response](Update(update, user, _))

      result.reply shouldBe Done
      result.event shouldBe Updated(update, user, testTime)
      result.state.asInstanceOf[Existing].webContent.title shouldBe "New title"
    }

    "return a validation error if the edits are invalid" in {
      val form = rootForm
      val user = superUser
      val update =
        UpdateForm(event = Some(WebCalendar(testTime, testTime.plusHours(1), "Venice", Venice)))

      val expectedErrors = List(
        ValidationError(
          "event",
          "Cannot insert an event in a non-calendar content",
          "content.notCalendar"
        )
      )

      rootTestKit.runCommand[Response](Create(form, user, _))
      eventually {
        rootTestKit.runCommand[Response](Read).reply shouldBe a[MyContent]
      }

      val result = rootTestKit.runCommand[Response](Update(update, user, _))

      result.reply shouldBe Invalid(expectedErrors)
    }

    "delete itself correctly" in {
      val form = rootForm
      val user = superUser

      rootTestKit.runCommand[Response](Create(form, user, _))
      val result = rootTestKit.runCommand[Response](Delete(user, _))
      result.reply shouldBe Done
      result.event shouldBe Deleted(user, testTime)
      result.state shouldBe NonExisting("/")

      rootTestKit.runCommand[Response](Read).reply shouldBe NotFound("/")
    }

    "correctly add a child" in {
      val form = rootForm
      val user = superUser
      val child = rootForm.copy(path = "/child", title = "Child").toChild

      rootTestKit.runCommand[Response](Create(form, user, _))
      val result = rootTestKit.runCommand[Response](AddChild(child, _))
      result.reply shouldBe Done
      result.event shouldBe ChildAdded(child)
      result.state.asInstanceOf[Existing].webContent.children should contain(child.path -> child)
    }

    "correctly update a child" in {
      val form = rootForm
      val user = superUser
      val child = rootForm.copy(path = "/child", title = "Child").toChild
      val child2 = child.copy(title = "New title")

      rootTestKit.runCommand[Response](Create(form, user, _))
      rootTestKit.runCommand[Response](AddChild(child, _))
      val result = rootTestKit.runCommand[Response](UpdateChild(child2, _))

      result.reply shouldBe Done
      result.event shouldBe ChildUpdated(child2)
      result.state.asInstanceOf[Existing].webContent.children should contain(child.path -> child2)
    }

    "correctly remove a child" in {
      val form = rootForm
      val user = superUser
      val child = rootForm.copy(path = "/child", title = "Child").toChild

      rootTestKit.runCommand[Response](Create(form, user, _))
      val result = rootTestKit.runCommand[Response](RemoveChild(child.path, _))
      result.reply shouldBe Done
      result.event shouldBe ChildRemoved(child.path)
      result.state.asInstanceOf[Existing].webContent.children shouldNot contain(child.path -> child)
    }

  }

  "The init function" should {
    "correctly initialize the actor" in {
      val ref = Content.init(system)

      ref shouldBe an[ActorRef[_]]
    }
  }

}
