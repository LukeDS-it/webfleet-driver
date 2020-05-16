package it.ldsoftware.webfleet.driver.service

import it.ldsoftware.webfleet.driver.actors.model.{CreationForm, EditingForm, WebContent}
import it.ldsoftware.webfleet.driver.security.User
import it.ldsoftware.webfleet.driver.service.model.{NoResult, ServiceResult}

import scala.concurrent.Future

trait ContentService {

  def getContent(path: String): Future[ServiceResult[WebContent]]

  def createContent(
      parentPath: String,
      form: CreationForm,
      user: User
  ): Future[ServiceResult[String]]

  def editContent(path: String, form: EditingForm, user: User): Future[ServiceResult[NoResult]]

  def deleteContent(path: String, user: User): Future[ServiceResult[NoResult]]

}
