package it.ldsoftware.webfleet.driver.actors

import java.time.ZonedDateTime

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import com.typesafe.config.{Config, ConfigFactory}
import it.ldsoftware.webfleet.driver.actors.model._
import it.ldsoftware.webfleet.driver.security.{Permissions, User}

class CQRSTestKit
    extends ScalaTestWithActorTestKit(
      EventSourcedBehaviorTestKit.config.withFallback(CQRSTestKit.config)
    ) {

  val testTime: ZonedDateTime = ZonedDateTime.now()

  val timeServer: TimeServer = new TimeServer {
    override def getTime: ZonedDateTime = testTime
  }

  val Venice: (Double, Double) = (45.438759, 12.327145)

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

  def getExpectedContent(form: CreateForm, user: User, expectedStatus: ContentStatus): WebContent =
    WebContent(
      form.title,
      form.path,
      form.webType,
      form.description,
      form.text,
      form.theme,
      form.icon,
      form.event,
      expectedStatus,
      user.name,
      Some(testTime),
      Some(testTime),
      Map()
    )

}

object CQRSTestKit {
  val config: Config = ConfigFactory.load("application")
}
