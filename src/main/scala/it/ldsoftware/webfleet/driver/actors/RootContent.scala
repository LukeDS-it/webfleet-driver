package it.ldsoftware.webfleet.driver.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect}
import it.ldsoftware.webfleet.driver.actors.model._
import it.ldsoftware.webfleet.driver.actors.validator.{ContentFormValidator, ValidationError}
import it.ldsoftware.webfleet.driver.security.User

object RootContent {

  sealed trait Command

  case class GetInfo(replyTo: ActorRef[Response]) extends Command

  case class AddChild(child: ContentForm, author: User, replyTo: ActorRef[Response]) extends Command

  case class EditChild(details: ContentChild, replyTo: ActorRef[Response]) extends Command

  case class RemoveChild(path: String, replyTo: ActorRef[Response]) extends Command

  case class EditContent(form: ContentForm, author: User, replyTo: ActorRef[Response]) extends Command

  sealed trait Event

  case class ChildAdded(child: ContentChild) extends Event

  case class ChildEdited(child: ContentChild) extends Event

  case class ChildRemoved(path: String) extends Event

  case class ContentEdited(newContent: ContentForm) extends Event

  sealed trait Response

  case object Done extends Response

  case class MyContent(webContent: WebContent) extends Response

  case class InvalidForm(validationErrors: List[ValidationError]) extends Response

  val validator: ContentFormValidator = new ContentFormValidator

  final case class State(
      title: String = "Home page",
      text: String = "",
      description: String = "Main page container",
      theme: String = "default",
      icon: String = "home.png",
      children: Set[ContentChild] = Set()
  ) {
    def handle(command: Command): ReplyEffect[Event, State] = command match {
      case GetInfo(replyTo)                   => Effect.reply(replyTo)(MyContent(toContent))
      case AddChild(child, author, replyTo)   => ???
      case EditChild(details, replyTo)        => Effect.persist(ChildEdited(details)).thenReply(replyTo)(_ => Done)
      case RemoveChild(path, replyTo)         => Effect.persist(ChildRemoved(path)).thenReply(replyTo)(_ => Done)
      case EditContent(form, author, replyTo) => ???
    }

    def process(event: Event): State = ???

    def toContent: WebContent =
      WebContent(
        title,
        "/",
        Folder,
        description,
        text,
        theme,
        icon,
        None,
        Published,
        "system",
        None,
        None,
        children
      )

  }

  def apply(): Behavior[Command] =
    EventSourcedBehavior
      .withEnforcedReplies[Command, Event, State](
        persistenceId = PersistenceId.ofUniqueId("/"),
        emptyState = State(),
        commandHandler = (state, command) => state.handle(command),
        eventHandler = (state, event) => state.process(event)
      )

}
