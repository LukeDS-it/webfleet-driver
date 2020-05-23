package it.ldsoftware.webfleet.driver.service.impl

import java.time.Duration

import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.util.Timeout
import it.ldsoftware.webfleet.driver.actors.model.{CreateForm, UpdateForm, WebContent}
import it.ldsoftware.webfleet.driver.actors.{Content, Creation}
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
      path: String,
      form: CreateForm,
      user: User
  ): Future[ServiceResult[NoResult]] =
    clusterSharding
      .entityRefFor(Creation.Key, path)
      .ask[Creation.Response](Creation.Start(form, user, _))
      .map {
        case Creation.Accepted => accepted
        case Creation.Refused  => invalid(List())
        case _                 => unexpectedMessage
      }

  override def editContent(
      path: String,
      form: UpdateForm,
      user: User
  ): Future[ServiceResult[NoResult]] =
    clusterSharding
      .entityRefFor(Content.Key, path)
      .ask[Content.Response](Content.Update(form, user, _))
      .map {
        case Content.Done                   => noOutput
        case Content.Invalid(errors)        => invalid(errors)
        case Content.NotFound(path)         => notFound(path)
        case Content.UnexpectedError(error) => unexpectedError(error, error.getMessage)
        case _                              => unexpectedMessage
      }

  override def deleteContent(path: String, user: User): Future[ServiceResult[NoResult]] =
    clusterSharding
      .entityRefFor(Content.Key, path)
      .ask[Content.Response](Content.Delete(user, _))
      .map {
        case Content.Done                   => noOutput
        case Content.Invalid(errors)        => invalid(errors)
        case Content.NotFound(path)         => notFound(path)
        case Content.UnexpectedError(error) => unexpectedError(error, error.getMessage)
        case Content.UnAuthorized           => forbidden
        case _                              => unexpectedMessage
      }

  private def unexpectedMessage[T]: ServiceResult[T] =
    unexpectedError(new Error(), "Unexpected response from actor")
}
