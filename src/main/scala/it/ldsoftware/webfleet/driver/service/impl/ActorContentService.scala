package it.ldsoftware.webfleet.driver.service.impl

import java.time.Duration

import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.util.Timeout
import it.ldsoftware.webfleet.driver.actors.Content
import it.ldsoftware.webfleet.driver.actors.model.{CreateForm, UpdateForm, WebContent}
import it.ldsoftware.webfleet.driver.security.User
import it.ldsoftware.webfleet.driver.service.ContentService
import it.ldsoftware.webfleet.driver.service.model._

import scala.concurrent.{ExecutionContext, Future}

class ActorContentService(
    askTimeout: Duration,
    clusterSharding: ClusterSharding
)(implicit ec: ExecutionContext)
    extends ContentService {

  implicit val timeout: Timeout = Timeout.create(askTimeout)

  override def getContent(path: String): Future[ServiceResult[WebContent]] =
    clusterSharding
      .entityRefFor(Content.Key, path)
      .ask[Content.Response](Content.Read)
      .map {
        case Content.MyContent(content) => success(content)
        case Content.NotFound(path)     => notFound(path)
        case _                          => unexpectedMessage
      }

  override def createContent(
      parentPath: String,
      form: CreateForm,
      user: User
  ): Future[ServiceResult[String]] = ???

  override def editContent(
      path: String,
      form: UpdateForm,
      user: User
  ): Future[ServiceResult[NoResult]] = ???

  override def deleteContent(path: String, user: User): Future[ServiceResult[NoResult]] = ???

  private def unexpectedMessage[T]: ServiceResult[T] =
    unexpectedError(new Error(), "Unexpected response from actor")
}
