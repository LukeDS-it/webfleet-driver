package it.ldsoftware.webfleet.driver.actors

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import akka.persistence.typed.{PersistenceId, RecoveryCompleted}
import it.ldsoftware.webfleet.driver.actors.model.{CreateForm, ValidationError}
import it.ldsoftware.webfleet.driver.security.User

/**
  * This class contains the implementation of the saga pattern for the creation of a
  * content. It will try to ensure consistency of the system by issuing commands to
  * the actual content actors to create the content and update the global tree, and
  * where this is not possible for validation errors or application crash, this will
  * also take care of rolling back everything or restarting the process where it left.
  */
object Creation {

  type Requester = ActorRef[Response]

  val Key: EntityTypeKey[Command] = EntityTypeKey[Command]("CreationSaga")

  sealed trait Command

  case object Stop extends Command
  case object Proceed extends Command
  case class GetState(replyTo: Requester) extends Command
  case class Fail(reason: String, errs: List[ValidationError]) extends Command
  case class Start(form: CreateForm, user: User, replyTo: Requester) extends Command

  sealed trait Event

  case class Started(form: CreateForm, user: User) extends Event
  case object TreeUpdated extends Event
  case class TreeFailed(reason: String, errs: List[ValidationError]) extends Event
  case object ContentCreated extends Event
  case class ContentFailed(reason: String, errs: List[ValidationError]) extends Event
  case object TreeRolledBack extends Event
  case object Succeeded extends Event

  sealed trait Response

  case object Accepted extends Response
  case object Refused extends Response
  case object SagaSuccess extends Response
  case class SagaStatus(code: String, completion: Int) extends Response
  case class SagaFailure(msg: String, err: List[ValidationError]) extends Response

  sealed trait State {
    def handle(command: Command, ctx: ActorContext[Command]): Effect[Event, State]
    def process(event: Event): State
    def resumeProcess(ctx: ActorContext[Command]): Unit
  }

  /**
    * In the idle state, the actor is waiting for a Start command to start the saga.
    * It can only receive a Start command and process a Started event. The next step
    * will always be AwaitingTree.
    *
    * There is no recover function from this state, as no command has been issued yet.
    */
  case object Idle extends State {
    override def handle(cmd: Command, ctx: ActorContext[Command]): Effect[Event, State] =
      cmd match {
        case Start(form, user, replyTo) =>
          Effect
            .persist(Started(form, user))
            .thenRun((_: State) => updateTree(form, ctx))
            .thenReply(replyTo)(_ => Accepted)
        case _ =>
          throw new IllegalArgumentException(s"Cannot process command $cmd")
      }

    override def process(event: Event): State = event match {
      case Started(form, user) => AwaitingTree(form, user)
      case _                   => throw new IllegalArgumentException
    }

    override def resumeProcess(ctx: ActorContext[Command]): Unit = {}
  }

  /**
    * In the AwaitingTree state, the actor is awaiting that the content tree is updated,
    * i.e. that the parent acknowledges that a new child has been added to the hierarchy.
    *
    * If the operation succeeds, a TreeUpdated event is saved. In this case, the next
    * state will be AwaitingContent. Also, as a side effect, the next step of the transaction
    * is started, issuing a create content command to the correct actor
    *
    * If the operation fails, a TreeFailed event is saved, along with the reasons.
    * In this case, the next state will be Failed.
    *
    * When the actor recoveries to this state, then it asks one more time to update the tree
    * to resume the transaction where it was left.
    *
    * @param form the creation form
    * @param user the user issuing the creation request
    */
  case class AwaitingTree(form: CreateForm, user: User) extends State {
    override def handle(cmd: Command, ctx: ActorContext[Command]): Effect[Event, State] =
      cmd match {
        case GetState(replyTo) =>
          Effect.reply(replyTo)(SagaStatus("stage.tree", 25))
        case Proceed =>
          Effect.persist(TreeUpdated).thenRun(_ => createContent(form, user, ctx))
        case Fail(reason, errs) =>
          Effect.persist(TreeFailed(reason, errs))
        case _ =>
          throw new IllegalArgumentException
      }

    override def process(event: Event): State = event match {
      case TreeUpdated              => AwaitingContent(form, user)
      case TreeFailed(reason, errs) => Failed(reason, errs)
      case _                        => throw new IllegalArgumentException
    }

    override def resumeProcess(ctx: ActorContext[Command]): Unit = updateTree(form, ctx)
  }

  /**
    * In this state, the actor is awaiting that the real content is created.
    *
    * If the operation succeeds, a ContentCreated event is saved. The next state will be Success
    * and no other side effects will be triggered.
    *
    * If the operation fails, a ContentFailed event is saved, along with the reasons.
    * The next state will be AwaitingRollback and as side effect, a request of removing a child from
    * the parent will be sent.
    *
    * When the actor recoveries to this state, it asks one more time to create the content.
    * @param form the creation form
    * @param user the user issuing the creation request
    */
  case class AwaitingContent(form: CreateForm, user: User) extends State {
    override def handle(command: Command, ctx: ActorContext[Command]): Effect[Event, State] =
      command match {
        case GetState(replyTo) =>
          Effect.reply(replyTo)(SagaStatus("stage.content", 50))
        case Proceed =>
          Effect.persist(ContentCreated)
        case Fail(reason, errs) =>
          Effect.persist(ContentFailed(reason, errs)).thenRun(_ => rollbackTree(form, ctx))
        case _ =>
          throw new IllegalArgumentException
      }

    override def process(event: Event): State = event match {
      case ContentCreated              => Completed
      case ContentFailed(reason, errs) => AwaitingRollback(form, reason, errs)
      case _                           => throw new IllegalArgumentException
    }

    override def resumeProcess(ctx: ActorContext[Command]): Unit = createContent(form, user, ctx)
  }

  /**
    * In this state, the actor is awaiting the rollback to the tree change because
    * there was some error in the process, and thus we require to rollback all changes.
    *
    * If the operation succeeds, a TreeRolledBack event is saved, the next state will then be
    * Failed.
    *
    * On resume, this actor will ask once more to the parent content to remove a child.
    *
    * @param form the form that triggered the creation, needed for the rollback
    * @param msg the error message
    * @param err list of validation errors that have caused the rollback
    */
  case class AwaitingRollback(form: CreateForm, msg: String, err: List[ValidationError])
      extends State {
    override def handle(command: Command, ctx: ActorContext[Command]): Effect[Event, State] =
      command match {
        case GetState(replyTo) => Effect.reply(replyTo)(SagaStatus("stage.rollback", 75))
        case Proceed           => Effect.persist(TreeRolledBack)
        case _                 => throw new IllegalArgumentException
      }

    override def process(event: Event): State = event match {
      case TreeRolledBack => Failed(msg, err)
      case _              => throw new IllegalArgumentException
    }

    override def resumeProcess(ctx: ActorContext[Command]): Unit = rollbackTree(form, ctx)
  }

  /**
    * This is a final state of the transaction, that indicates success.
    *
    * Resuming this actor brings it to immediate termination in order to avoid resource waste.
    */
  case object Completed extends State {
    override def handle(command: Command, ctx: ActorContext[Command]): Effect[Event, State] =
      command match {
        case GetState(r) => Effect.reply(r)(SagaSuccess)
        case Stop        => Effect.stop()
        case _           => throw new IllegalArgumentException
      }

    override def process(event: Event): State = throw new IllegalArgumentException

    override def resumeProcess(ctx: ActorContext[Command]): Unit = stop(ctx)
  }

  /**
    * This is a final state of the transaction, that indicates that the transaction has failed
    * and has been rolled back.
    *
    * Resuming this actor brings it to immediate termination in order to avoid resource waste.
    */
  case class Failed(reason: String, errors: List[ValidationError]) extends State {
    override def handle(command: Command, ctx: ActorContext[Command]): Effect[Event, State] =
      command match {
        case GetState(r) => Effect.reply(r)(SagaFailure(reason, errors))
        case Stop        => Effect.stop()
        case _           => throw new IllegalArgumentException
      }

    override def process(event: Event): State = throw new IllegalArgumentException

    override def resumeProcess(ctx: ActorContext[Command]): Unit = stop(ctx)
  }

  def apply(id: String): Behavior[Command] = Behaviors.setup { ctx =>
    EventSourcedBehavior[Command, Event, State](
      PersistenceId.ofUniqueId(id),
      Idle,
      (state, command) => state.handle(command, ctx),
      (state, event) => state.process(event)
    ).receiveSignal {
      case (state, RecoveryCompleted) => state.resumeProcess(ctx)
    }
  }

  def init(system: ActorSystem[_]): ActorRef[_] =
    ClusterSharding(system).init(Entity(Key) { context => Creation(context.entityId) })

  /**
    * Triggers the creation of a new element in the tree structure
    */
  private def updateTree(form: CreateForm, ctx: ActorContext[Command]): Unit = {
    val child = form.toChild
    ClusterSharding(ctx.system)
      .entityRefFor(Content.Key, child.getParentPath)
      .tell(Content.AddChild(child, self(ctx)))
  }

  /**
    * Triggers the creation of a new content
    */
  private def createContent(form: CreateForm, user: User, ctx: ActorContext[Command]): Unit =
    ClusterSharding(ctx.system)
      .entityRefFor(Content.Key, form.path)
      .tell(Content.Create(form, user, self(ctx)))

  /**
    * Triggers a rollback on the tree structure
    */
  private def rollbackTree(form: CreateForm, ctx: ActorContext[Command]): Unit = {
    val child = form.toChild
    ClusterSharding(ctx.system)
      .entityRefFor(Content.Key, child.getParentPath)
      .tell(Content.RemoveChild(child.path, self(ctx)))
  }

  /**
    * Issues the stop command to the current actor
    */
  private def stop(ctx: ActorContext[Command]): Unit = ctx.self ! Stop

  /**
    * Creates an anonymous actor that acts as a message relay and translator.
    * It translates responses of the Content actor and creates commands to give to this Creation
    * saga actor.
    *
    * @param ctx the context of the current actor
    * @return an actor ref that can be passed as replyTo argument for Content commands
    */
  private def self(ctx: ActorContext[Command]): ActorRef[Content.Response] = ctx.spawnAnonymous(
    Behaviors.receiveMessage[Content.Response] { msg =>
      val next = msg match {
        case Content.Done                   => Proceed
        case Content.MyContent(_)           => throw new IllegalArgumentException
        case Content.Invalid(errors)        => Fail("Validation errors", errors)
        case Content.NotFound(path)         => Fail(s"Content not found $path", Nil)
        case Content.UnexpectedError(error) => Fail(error.getMessage, Nil)
        case Content.UnAuthorized           => Fail("Unauthorized", Nil)
      }
      ctx.self ! next
      Behaviors.stopped
    }
  )
}
