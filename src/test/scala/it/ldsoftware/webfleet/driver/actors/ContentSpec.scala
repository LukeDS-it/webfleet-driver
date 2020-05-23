package it.ldsoftware.webfleet.driver.actors

import java.time.ZonedDateTime

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import com.typesafe.config.{Config, ConfigFactory}
import it.ldsoftware.webfleet.driver.actors.Content._
import it.ldsoftware.webfleet.driver.actors.model._
import it.ldsoftware.webfleet.driver.security.{Permissions, User}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.wordspec.AnyWordSpecLike

class ContentSpec
    extends ScalaTestWithActorTestKit(
      EventSourcedBehaviorTestKit.config.withFallback(ContentSpec.config)
    )
    with AnyWordSpecLike
    with BeforeAndAfterEach {

  private val testTime = ZonedDateTime.now()

  private val timeServer = new TimeServer {
    override def getTime: ZonedDateTime = testTime
  }

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
      val result = rootTestKit.runCommand[Response](Create(form, user, _))
      result.reply shouldBe Done
      result.event shouldBe Created(form, user, testTime)
    }

    "do?" in {}

  }

  def rootForm: CreateForm = CreateForm(
    "Title",
    "/",
    Folder,
    "Website root",
    "Sample text",
    "default",
    "icon",
    Some(Published),
    None
  )

  def superUser: User = User("name", Permissions.all, None)

}

object ContentSpec {
  val config: Config = ConfigFactory.load("application")
}
