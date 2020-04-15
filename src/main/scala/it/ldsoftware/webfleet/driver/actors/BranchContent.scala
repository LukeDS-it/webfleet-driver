package it.ldsoftware.webfleet.driver.actors

import java.time.ZonedDateTime

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect}
import akka.util.Timeout
import it.ldsoftware.webfleet.driver.actors.RootContent._
import it.ldsoftware.webfleet.driver.actors.errors.UnexpectedResponseException
import it.ldsoftware.webfleet.driver.actors.model._
import it.ldsoftware.webfleet.driver.actors.validators.{EditContentValidator, NewContentValidator}
import it.ldsoftware.webfleet.driver.security.{Permissions, User}

import scala.util.{Failure, Success}

object BranchContent {

  // format: off
  sealed trait BranchCommand {
    val replyTo: ActorRef[BranchResponse]
  }
  // --- Public commands, can be received from the outside environment
  case class GetBranchInfo(replyTo: ActorRef[BranchResponse]) extends BranchCommand
  case class AddBranchContent(child: CreationForm, author: User, replyTo: ActorRef[BranchResponse]) extends BranchCommand
  case class EditBranchContent(form: EditingForm, author: User, replyTo: ActorRef[BranchResponse]) extends BranchCommand
  case class DeleteBranch(user: User, replyTo: ActorRef[BranchResponse]) extends BranchCommand
  // --- Commands that can be received only from other actors because do not require validation
  private[actors] case class InitializeBranch(form: CreationForm, author: User, replyTo: ActorRef[BranchResponse]) extends BranchCommand
  private[actors] case class UpdateBranchChild(details: ContentChild, replyTo: ActorRef[BranchResponse]) extends BranchCommand
  private[actors] case class RemoveBranchChild(path: String, replyTo: ActorRef[BranchResponse]) extends BranchCommand
  // --- Commands that can be sent only internally from this actor
  private case class FatherUpdated(replyTo: ActorRef[BranchResponse]) extends BranchCommand
  private case class AddBranchChild(child: ContentChild, replyTo: ActorRef[BranchResponse]) extends BranchCommand
  private case class BranchReplyFailure(throwable: Throwable, replyTo: ActorRef[BranchResponse]) extends BranchCommand

  sealed trait BranchEvent
  case class BranchContentInitialized(content: WebContent) extends BranchEvent
  case class BranchChildAdded(child: ContentChild) extends BranchEvent
  case class BranchChildEdited(child: ContentChild) extends BranchEvent
  case class BranchChildRemoved(path: String) extends BranchEvent
  case class BranchEdited(newContent: EditingForm) extends BranchEvent
  case object BranchDeleted extends BranchEvent

  sealed trait BranchResponse
  case object BranchDone extends BranchResponse
  case object BranchNotFound extends BranchResponse
  case object Initialized extends BranchResponse
  case object InvalidCommand extends BranchResponse
  case class BranchContentResponse(webContent: WebContent) extends BranchResponse
  case class InvalidBranchForm(validationErrors: List[ValidationError]) extends BranchResponse
  case class UnexpectedBranchFailure(ex: Throwable) extends BranchResponse
  case object InsufficientBranchPermissions extends BranchResponse

  val BranchKey: EntityTypeKey[BranchCommand] = EntityTypeKey[BranchCommand]("BranchContent")
  // format: on

  sealed trait State {
    def handle(command: BranchCommand, ctx: ActorContext[BranchCommand])(
        implicit timeout: Timeout
    ): ReplyEffect[BranchEvent, State]

    def process(event: BranchEvent): State
  }

  final case class NonExisting() extends State {
    override def handle(command: BranchCommand, ctx: ActorContext[BranchCommand])(
        implicit timeout: Timeout
    ): ReplyEffect[BranchEvent, State] = command match {
      case InitializeBranch(form, author, replyTo) =>
        Effect
          .persist(BranchContentInitialized(build(form, author)))
          .thenReply(replyTo)(_ => Initialized)
      case other =>
        Effect.reply(other.replyTo)(BranchNotFound)
    }

    override def process(event: BranchEvent): State = event match {
      case BranchContentInitialized(content) => Existing(content)
      case _                                 => NonExisting()
    }

    def build(form: CreationForm, user: User): WebContent = {
      val date = ZonedDateTime.now()
      val status = calculateStatus(form, user)

      WebContent(
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
    }

    def calculateStatus(form: CreationForm, user: User): ContentStatus =
      form.contentStatus
        .map {
          case Published =>
            if (user.permissions.contains(Permissions.Contents.Publish)) Published
            else Review
          case other => other
        }
        .getOrElse(Stub)
  }

  final case class Existing(content: WebContent) extends State {

    private val newContentValidator = new NewContentValidator()
    private val editContentValidator = new EditContentValidator()

    override def handle(command: BranchCommand, ctx: ActorContext[BranchCommand])(
        implicit timeout: Timeout
    ): ReplyEffect[BranchEvent, State] = command match {
      case GetBranchInfo(replyTo) => Effect.reply(replyTo)(BranchContentResponse(content))

      case cmd: AddBranchContent =>
        newContentValidator.validate(cmd.child, content) match {
          case Nil => Effect.none.thenRun(initChild(ctx, cmd)).thenNoReply()
          case err => Effect.none.thenReply(cmd.replyTo)(_ => InvalidBranchForm(err))
        }

      case EditBranchContent(form, _, replyTo) =>
        editContentValidator.validate(form, content) match {
          case Nil => Effect.none.thenRun(editSelf(ctx, form, replyTo)).thenNoReply()
          case err => Effect.none.thenReply(replyTo)(_ => InvalidBranchForm(err))
        }

      case DeleteBranch(user, replyTo) =>
        if (user.permissions.contains(Permissions.Contents.Review) || user.name == content.author)
          Effect
            .persist(BranchDeleted)
            .thenRun(deleteChildren(user, ctx))
            .thenReply(replyTo)(_ => BranchDone)
        else
          Effect.none.thenReply(replyTo)(_ => InsufficientBranchPermissions)

      case InitializeBranch(_, _, replyTo) => Effect.reply(replyTo)(InvalidCommand)

      case AddBranchChild(child, replyTo) =>
        Effect.persist(BranchChildAdded(child)).thenReply(replyTo)(_ => BranchDone)

      case UpdateBranchChild(details, replyTo) =>
        Effect.persist(BranchChildEdited(details)).thenReply(replyTo)(_ => BranchDone)

      case RemoveBranchChild(path, replyTo) =>
        Effect.persist(BranchChildRemoved(path)).thenReply(replyTo)(_ => BranchDone)

      case BranchReplyFailure(ex, replyTo) => Effect.reply(replyTo)(UnexpectedBranchFailure(ex))

      case FatherUpdated(replyTo) => Effect.reply(replyTo)(BranchDone)
    }

    override def process(event: BranchEvent): State = event match {
      case BranchContentInitialized(_) => this

      case BranchChildAdded(child) =>
        Existing(
          this.content.copy(children = this.content.children + (child.path -> child))
        )

      case BranchChildEdited(child) =>
        Existing(
          this.content.copy(children = this.content.children + (child.path -> child))
        )

      case BranchChildRemoved(path) =>
        Existing(this.content.copy(children = this.content.children - path))

      case BranchEdited(newContent) =>
        val p =
          if (this.content.published.isDefined)
            this.content.published
          else if (newContent.status == Published)
            Some(ZonedDateTime.now())
          else
            None

        Existing(
          this.content.copy(
            title = newContent.title,
            description = newContent.description,
            text = newContent.text,
            theme = newContent.theme,
            icon = newContent.icon,
            event = newContent.event,
            status = newContent.status,
            published = p
          )
        )

      case BranchDeleted => NonExisting()
    }

    def initChild(ctx: ActorContext[BranchCommand], cmd: AddBranchContent)(
        implicit timeout: Timeout
    ): State => Unit = _ => {
      val initialize = ClusterSharding(ctx.system)
        .entityRefFor(BranchKey, cmd.child.path)
        .ask[BranchResponse](InitializeBranch(cmd.child, cmd.author, _))

      ctx.pipeToSelf(initialize) {
        case Success(value) =>
          value match {
            case Initialized => AddBranchChild(cmd.child.toChild, cmd.replyTo)
            case _           => BranchReplyFailure(new UnexpectedResponseException(), cmd.replyTo)
          }
        case Failure(ex) => BranchReplyFailure(ex, cmd.replyTo)
      }
    }

    def editSelf(
        ctx: ActorContext[BranchCommand],
        form: EditingForm,
        replyTo: ActorRef[BranchResponse]
    )(
        implicit timeout: Timeout
    ): State => Unit = _ => {
      val contentChild =
        ContentChild(this.content.path, form.title, form.description, this.content.webType)
      val fatherPath = this.content.path.substring(0, this.content.path.lastIndexOf("/"))

      if (fatherPath == "/") {
        val r = ClusterSharding(ctx.system)
          .entityRefFor(RootKey, fatherPath)
          .ask[RootResponse](UpdateRootChild(contentChild, _))

        ctx.pipeToSelf(r) {
          case Success(value) =>
            value match {
              case RootDone                  => FatherUpdated(replyTo)
              case UnexpectedRootFailure(ex) => BranchReplyFailure(ex, replyTo)
              case _                         => BranchReplyFailure(new UnexpectedResponseException(), replyTo)
            }
          case Failure(ex) => BranchReplyFailure(ex, replyTo)
        }
      } else {
        val r = ClusterSharding(ctx.system)
          .entityRefFor(BranchKey, fatherPath)
          .ask[BranchResponse](UpdateBranchChild(contentChild, _))

        ctx.pipeToSelf(r) {
          case Success(value) =>
            value match {
              case BranchDone                  => FatherUpdated(replyTo)
              case UnexpectedBranchFailure(ex) => BranchReplyFailure(ex, replyTo)
              case _                           => BranchReplyFailure(new UnexpectedResponseException(), replyTo)
            }
          case Failure(ex) => BranchReplyFailure(ex, replyTo)
        }
      }

    }

    def deleteChildren(user: User, ctx: ActorContext[BranchCommand]): State => Unit = { _ =>
      this.content.children.keys.foreach { path =>
        ClusterSharding(ctx.system)
          .entityRefFor(BranchKey, path)
          .tell(DeleteBranch(user, null))
      }
    }

  }

  def apply(id: String, timeout: Timeout): Behavior[BranchCommand] =
    Behaviors.setup[BranchCommand] { context =>
      implicit val t: Timeout = timeout
      EventSourcedBehavior
        .withEnforcedReplies[BranchCommand, BranchEvent, State](
          persistenceId = PersistenceId.ofUniqueId(id),
          emptyState = NonExisting(),
          commandHandler = (state, command) => state.handle(command, context),
          eventHandler = (state, event) => state.process(event)
        )
    }

  def init(system: ActorSystem[_], timeout: Timeout): ActorRef[_] =
    ClusterSharding(system).init(Entity(BranchKey) { context =>
      BranchContent(context.entityId, timeout)
    })

}
