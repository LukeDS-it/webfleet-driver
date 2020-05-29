package it.ldsoftware.webfleet.driver.actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior, PostStop}
import akka.cluster.sharding.typed.scaladsl.ShardedDaemonProcess
import akka.stream.KillSwitches
import it.ldsoftware.webfleet.driver.flows.ContentFlow

object EventProcessor {

  def apply(flow: ContentFlow): Behavior[Nothing] = Behaviors.setup[Nothing] { _ =>
    val killSwitch = KillSwitches.shared("eventProcessorSwitch")
    flow.run(killSwitch)
    Behaviors.receiveSignal[Nothing] {
      case (_, PostStop) =>
        killSwitch.shutdown()
        Behaviors.same
    }
  }

  def init(
      system: ActorSystem[_],
      flow: ContentFlow
  ): Unit = {
    ShardedDaemonProcess(system).init[Nothing](
      "event-processors-content",
      1,
      _ => EventProcessor(flow)
    )
  }

}
