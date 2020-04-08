package it.ldsoftware.webfleet.driver.actors

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}

object GreeterActor {

  sealed trait Command
  case class Greet(name: String, replyTo: ActorRef[Response]) extends Command

  sealed trait Event

  final case class State()

  sealed trait Response
  case class HelloResponse(response: String) extends Response

  val EntityKey: EntityTypeKey[Command] = EntityTypeKey[Command]("GreeterActor")

  def apply(): Behavior[Command] = EventSourcedBehavior[Command, Event, State](
    persistenceId = PersistenceId.ofUniqueId("greeter"),
    emptyState = State(),
    commandHandler = (_, cmd) => {
      cmd match {
        case Greet(name, replyTo) => Effect.none.thenReply(replyTo)(_ => HelloResponse(s"Hello $name"))
      }
    },
    eventHandler = (_, _) => throw new NotImplementedError()
  )

  def init(system: ActorSystem[_]): Unit = {
    ClusterSharding(system).init(Entity(EntityKey) { _ => GreeterActor() })
  }

}
