package it.ldsoftware.webfleet.driver.service.impl

import java.time.Duration

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.util.Timeout
import it.ldsoftware.webfleet.driver.actors.GreeterActor
import it.ldsoftware.webfleet.driver.service.GreeterService
import it.ldsoftware.webfleet.driver.service.model._

import scala.concurrent.{ExecutionContext, Future}

class ActorGreeterService(askTimeout: Duration)(
    implicit system: ActorSystem[_],
    ec: ExecutionContext
) extends GreeterService {

  private val sharding: ClusterSharding = ClusterSharding(system)

  implicit val timeout: Timeout = Timeout.create(askTimeout)

  override def greet(name: String): Future[ServiceResult[String]] =
    sharding
      .entityRefFor(GreeterActor.EntityKey, "greeter")
      .ask[GreeterActor.Response](GreeterActor.Greet(name, _))
      .map {
        case GreeterActor.HelloResponse(response) => success(response)
      }
      .recover(th => unexpectedError(th, "An unexpected error has occurred"))
}
