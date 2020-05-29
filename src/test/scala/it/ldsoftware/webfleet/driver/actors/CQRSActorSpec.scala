package it.ldsoftware.webfleet.driver.actors

import java.time.ZonedDateTime

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity}
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import com.typesafe.config.{Config, ConfigFactory}
import it.ldsoftware.webfleet.driver.actors.model._
import it.ldsoftware.webfleet.driver.security.{Permissions, User}

class CQRSActorSpec
    extends ScalaTestWithActorTestKit(
      EventSourcedBehaviorTestKit.config.withFallback(CQRSActorSpec.config)
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

  def superUser: User = User("name", Permissions.AllPermissions, None)

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

  def mockContent(commandTranslator: Content.Command => Content.Response): Unit = {
    val behavior = Behaviors.receiveMessage[Content.Command] { msg =>
      val resp = commandTranslator(msg)
      msg.replyTo ! resp
      Behaviors.same
    }

    ClusterSharding(system).init(Entity(Content.Key) { _ => behavior })
  }

}

object CQRSActorSpec {
  val config: Config = ConfigFactory.load("application")
}
