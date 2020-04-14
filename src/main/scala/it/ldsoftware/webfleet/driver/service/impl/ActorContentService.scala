package it.ldsoftware.webfleet.driver.service.impl

import java.time.Duration

import akka.actor.typed.ActorSystem
import it.ldsoftware.webfleet.driver.actors.model.{CreationForm, EditingForm, WebContent}
import it.ldsoftware.webfleet.driver.security.User
import it.ldsoftware.webfleet.driver.service.ContentService
import it.ldsoftware.webfleet.driver.service.model.{NoResult, ServiceResult}

import scala.concurrent.{ExecutionContext, Future}

class ActorContentService(askTimeout: Duration)(
    implicit system: ActorSystem[_],
    ec: ExecutionContext
) extends ContentService {

  override def getContent(path: String): Future[ServiceResult[WebContent]] = ???

  override def createContent(
      parentPath: String,
      form: CreationForm,
      user: User
  ): Future[ServiceResult[String]] = ???

  override def editContent(
      path: String,
      form: EditingForm,
      user: User
  ): Future[ServiceResult[NoResult]] = ???

  override def deleteContent(path: String, user: User): Future[ServiceResult[NoResult]] = ???
}
