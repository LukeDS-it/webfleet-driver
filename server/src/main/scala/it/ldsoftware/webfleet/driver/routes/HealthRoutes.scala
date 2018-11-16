package it.ldsoftware.webfleet.driver.routes

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import it.ldsoftware.webfleet.driver.services.KafkaService
import spray.json.DefaultJsonProtocol

trait HealthRoutes extends SprayJsonSupport with DefaultJsonProtocol {

  def kafkaService: KafkaService

  def healthRoute: Route = path("api" / "v1" / "health") {
    get {
      complete {
        val statuses = Map("webfleet-driver" -> StatusCodes.OK, "kafka" -> kafkaService.getHealth)

        val overall = statuses.values.fold(StatusCodes.OK) {
          case (end, curr) =>
            if (end.isFailure || curr.isFailure) StatusCodes.ServiceUnavailable
            else StatusCodes.OK
        }

        overall -> statuses.mapValues(_.value)
      }
    }
  }

}

object HealthRoutes {
  val healthPath: String = "/api/v1/health"
}