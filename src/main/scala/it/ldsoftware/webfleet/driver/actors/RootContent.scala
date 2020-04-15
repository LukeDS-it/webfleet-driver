package it.ldsoftware.webfleet.driver.actors

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect}
import akka.util.Timeout
import it.ldsoftware.webfleet.driver.actors.BranchContent.Initialized
import it.ldsoftware.webfleet.driver.actors.errors.UnexpectedResponseException
import it.ldsoftware.webfleet.driver.actors.model._
import it.ldsoftware.webfleet.driver.actors.validators.{EditContentValidator, NewContentValidator}
import it.ldsoftware.webfleet.driver.security.User

import scala.util.{Failure, Success}

object RootContent {

  // format: off
  sealed trait RootCommand

  // --- Public commands, can be received from the outside environment
  case class GetRootInfo(replyTo: ActorRef[RootResponse]) extends RootCommand
  case class AddRootChild(child: CreationForm, author: User, replyTo: ActorRef[RootResponse]) extends RootCommand
  case class EditRootContent(form: EditingForm, author: User, replyTo: ActorRef[RootResponse])  extends RootCommand
  // --- Commands that can be received only from other actors because do not require validation
  private[actors] case class RemoveRootChild(path: String, replyTo: ActorRef[RootResponse]) extends RootCommand
  private[actors] case class UpdateRootChild(child: ContentChild, replyTo: ActorRef[RootResponse]) extends RootCommand
  // --- Commands that can be sent only internally from this actor
  private case class AddChild(child: ContentChild, replyTo: ActorRef[RootResponse]) extends RootCommand
  private case class ReplyFailure(throwable: Throwable, replyTo: ActorRef[RootResponse]) extends RootCommand

  sealed trait RootEvent
  case class ChildAdded(child: ContentChild) extends RootEvent
  case class ChildEdited(child: ContentChild) extends RootEvent
  case class ChildRemoved(path: String) extends RootEvent
  case class ContentEdited(newContent: EditingForm) extends RootEvent

  sealed trait RootResponse
  case object RootDone extends RootResponse
  case class RootContentResponse(webContent: WebContent) extends RootResponse
  case class InvalidForm(validationErrors: List[ValidationError]) extends RootResponse
  case class UnexpectedRootFailure(ex: Throwable) extends RootResponse
  case object InsufficientRootPermissions extends RootResponse

  val RootKey: EntityTypeKey[RootCommand] = EntityTypeKey[RootCommand]("RootContent")
  // format: on

  final case class State(
      title: String = "Home page",
      text: String = "",
      description: String = "Main page container",
      theme: String = "default",
      icon: String = "home.png",
      children: Map[String, ContentChild] = Map()
  ) {

    private val newContentValidator = new NewContentValidator()
    private val editContentValidator = new EditContentValidator()

    def handle(command: RootCommand, ctx: ActorContext[RootCommand])(
        implicit timeout: Timeout
    ): ReplyEffect[RootEvent, State] =
      command match {
        case GetRootInfo(replyTo) =>
          Effect.reply(replyTo)(RootContentResponse(toContent))

        case cmd: AddRootChild =>
          newContentValidator.validate(cmd.child, toContent) match {
            case Nil => Effect.none.thenRun(initChild(ctx, cmd)).thenNoReply()
            case err => Effect.none.thenReply(cmd.replyTo)(_ => InvalidForm(err))
          }

        case UpdateRootChild(details, replyTo) =>
          Effect.persist(ChildEdited(details)).thenReply(replyTo)(_ => RootDone)

        case RemoveRootChild(path, replyTo) =>
          Effect.persist(ChildRemoved(path)).thenReply(replyTo)(_ => RootDone)

        case EditRootContent(form, _, replyTo) =>
          editContentValidator.validate(form, toContent) match {
            case Nil => Effect.persist(ContentEdited(form)).thenReply(replyTo)(_ => RootDone)
            case err => Effect.none.thenReply(replyTo)(_ => InvalidForm(err))
          }

        case AddChild(child, replyTo) =>
          Effect.persist(ChildAdded(child)).thenReply(replyTo)(_ => RootDone)

        case ReplyFailure(th, replyTo) =>
          Effect.none.thenReply(replyTo)(_ => UnexpectedRootFailure(th))
      }

    def process(event: RootEvent): State = event match {
      case ChildAdded(child)  => this.copy(children = children + (child.path -> child))
      case ChildEdited(child) => this.copy(children = children + (child.path -> child))
      case ChildRemoved(path) => this.copy(children = children - path)
      case ContentEdited(newContent) =>
        this.copy(
          title = newContent.title,
          text = newContent.text,
          description = newContent.description,
          theme = newContent.theme,
          icon = newContent.icon
        )
    }

    def initChild(ctx: ActorContext[RootCommand], cmd: AddRootChild)(
        implicit timeout: Timeout
    ): State => Unit = _ => {
      val initialize = ClusterSharding(ctx.system)
        .entityRefFor(BranchContent.BranchKey, cmd.child.path)
        .ask[BranchContent.BranchResponse](BranchContent.InitializeBranch(cmd.child, cmd.author, _))

      ctx.pipeToSelf(initialize) {
        case Success(value) =>
          value match {
            case Initialized => AddChild(cmd.child.toChild, cmd.replyTo)
            case _           => ReplyFailure(new UnexpectedResponseException(), cmd.replyTo)
          }
        case Failure(ex) => ReplyFailure(ex, cmd.replyTo)
      }
    }

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

  def apply(timeout: Timeout): Behavior[RootCommand] = Behaviors.setup[RootCommand] { context =>
    implicit val t: Timeout = timeout
    EventSourcedBehavior
      .withEnforcedReplies[RootCommand, RootEvent, State](
        persistenceId = PersistenceId.ofUniqueId("/"),
        emptyState = State(),
        commandHandler = (state, command) => state.handle(command, context),
        eventHandler = (state, event) => state.process(event)
      )
  }

  def init(system: ActorSystem[_], timeout: Timeout): ActorRef[_] =
    ClusterSharding(system).init(Entity(RootKey) { _ => RootContent(timeout) })

}
