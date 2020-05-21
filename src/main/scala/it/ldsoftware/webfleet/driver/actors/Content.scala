package it.ldsoftware.webfleet.driver.actors

import java.time.ZonedDateTime

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect}
import it.ldsoftware.webfleet.driver.actors.model._
import it.ldsoftware.webfleet.driver.security.{Permissions, User}

/**
  * This object contains actor logic for any content of the website.
  */
object Content {

  type Requester = ActorRef[Response]

  val Key: EntityTypeKey[Command] = EntityTypeKey[Command]("WebContent")

  sealed trait Command {
    val replyTo: Requester
  }

  case class Read(replyTo: Requester) extends Command
  case class Create(form: CreateForm, user: User, replyTo: Requester) extends Command
  case class Update(form: UpdateForm, user: User, replyTo: Requester) extends Command
  case class Delete(user: User, replyTo: Requester) extends Command
  case class AddChild(child: ContentChild, replyTo: Requester) extends Command
  case class UpdateChild(child: ContentChild, replyTo: Requester) extends Command
  case class RemoveChild(child: String, replyTo: Requester) extends Command

  sealed trait Event

  case class Created(form: CreateForm, user: User, time: ZonedDateTime) extends Event
  case class Updated(form: UpdateForm, user: User, time: ZonedDateTime) extends Event
  case class Deleted(user: User, timestamp: ZonedDateTime) extends Event
  case class ChildAdded(child: ContentChild) extends Event
  case class ChildUpdated(child: ContentChild) extends Event
  case class ChildRemoved(child: String) extends Event

  sealed trait Response

  case class MyContent(content: WebContent) extends Response
  case class Invalid(errors: List[ValidationError]) extends Response
  case class NotFound(path: String) extends Response
  case class UnexpectedError(error: Throwable) extends Response
  case object Done extends Response
  case object UnAuthorized extends Response
  private def Duplicate(str: String) =
    Invalid(List(ValidationError("path", s"Content at $str already exists", "path.duplicate")))

  /**
    * The state will know how to handle commands and process events, so that logic will be easier
    * to test and is logically organized
    */
  sealed trait State {
    def handle(command: Command): ReplyEffect[Event, State]
    def process(event: Event): State
  }

  /**
    * This state represents a non existing content. The only things it can do are
    * accepting and validating a creation request, or responding "not found" to
    * all other requests.
    *
    * It can only respond to a Created event to create a new Existing state
    *
    * @param path the http relative path to this content
    */
  case class NonExisting(path: String) extends State {
    override def handle(command: Command): ReplyEffect[Event, State] = command match {
      case Create(form, user, replyTo) =>
        form.validationErrors match {
          case Nil =>
            Effect.persist(Created(form, user, ZonedDateTime.now())).thenReply(replyTo)(_ => Done)
          case err =>
            Effect.reply(replyTo)(Invalid(err))
        }
      case _ => Effect.reply(command.replyTo)(NotFound(path))
    }

    override def process(event: Event): State = event match {
      case Created(form, user, time) => Existing(form, user, time)
      case _                         => throw new IllegalStateException(s"Cannot process $event")
    }
  }

  /**
    * This state represents an existing content. It will reply its current state when requested;
    * accept update, delete, add child, remove child, update child requests; it will refuse
    * other creation requests.
    *
    * @param webContent the actual content of an existing Content
    */
  case class Existing(webContent: WebContent) extends State {

    override def handle(command: Command): ReplyEffect[Event, State] = command match {
      case Read(replyTo) =>
        Effect.reply(replyTo)(MyContent(webContent))
      case Create(_, _, replyTo) =>
        Effect.reply(replyTo)(Duplicate(webContent.path))
      case Update(form, user, replyTo) =>
        form.validationErrors(webContent) match {
          case Nil =>
            Effect.persist(Updated(form, user, ZonedDateTime.now())).thenReply(replyTo)(_ => Done)
          case err =>
            Effect.reply(replyTo)(Invalid(err))
        }
      case Delete(user, replyTo) =>
        Effect.persist(Deleted(user, ZonedDateTime.now())).thenReply(replyTo)(_ => Done)
      case AddChild(child, replyTo) =>
        Effect.persist(ChildAdded(child)).thenReply(replyTo)(_ => Done)
      case UpdateChild(child, replyTo) =>
        Effect.persist(ChildUpdated(child)).thenReply(replyTo)(_ => Done)
      case RemoveChild(child, replyTo) =>
        Effect.persist(ChildRemoved(child)).thenReply(replyTo)(_ => Done)
    }

    override def process(event: Event): State = event match {
      case Updated(form, _, time) =>
        Existing(
          webContent.copy(
            title = form.title.getOrElse(webContent.title),
            description = form.description.getOrElse(webContent.description),
            text = form.text.getOrElse(webContent.text),
            theme = form.theme.getOrElse(webContent.theme),
            icon = form.icon.getOrElse(webContent.icon),
            event = form.event.orElse(webContent.event),
            status = form.status.getOrElse(webContent.status),
            published =
              if (webContent.status != Published && form.status.forall(_ == Published))
                Some(time)
              else
                webContent.published
          )
        )
      case Deleted(_, _) =>
        NonExisting(webContent.path)
      case ChildAdded(child) =>
        Existing(webContent.copy(children = webContent.children + (child.path -> child)))
      case ChildUpdated(child) =>
        Existing(webContent.copy(children = webContent.children + (child.path -> child)))
      case ChildRemoved(child) =>
        Existing(webContent.copy(children = webContent.children - child))
      case e =>
        throw new IllegalStateException(s"Cannot process $e")
    }
  }

  case object Existing {
    def apply(form: CreateForm, user: User, date: ZonedDateTime): Existing = {
      val status = form.contentStatus
        .map {
          case Published =>
            if (user.permissions.contains(Permissions.Contents.Publish)) Published
            else Review
          case other => other
        }
        .getOrElse(Stub)

      val content = WebContent(
        form.title,
        form.path,
        form.webType,
        form.description,
        form.text,
        form.theme,
        form.icon,
        form.event,
        status,
        user.name,
        Some(date),
        if (status == Published) Some(date) else None,
        Map()
      )

      Existing(content)
    }

  }

  /**
    * This creates the behavior for the Content actor
    *
    * @param id the path of this actor, which corresponds to the http relative path to the content
    * @return the behavior of the Content actor
    */
  def apply(id: String): Behavior[Command] =
    EventSourcedBehavior
      .withEnforcedReplies[Command, Event, State](
        persistenceId = PersistenceId.ofUniqueId(id),
        emptyState = NonExisting(id),
        commandHandler = (state, command) => state.handle(command),
        eventHandler = (state, event) => state.process(event)
      )

  /**
    * This function initializes the Content actor in the cluster sharding
    *
    * @param system the main actor system
    * @return the reference to the Content actor
    */
  def init(system: ActorSystem[_]): ActorRef[_] =
    ClusterSharding(system).init(Entity(Key) { context => Content(context.entityId) })
}
