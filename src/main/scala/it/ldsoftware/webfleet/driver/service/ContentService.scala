package it.ldsoftware.webfleet.driver.service

import it.ldsoftware.webfleet.driver.actors.model.{CreateForm, UpdateForm, WebContent}
import it.ldsoftware.webfleet.driver.security.User
import it.ldsoftware.webfleet.driver.service.model.{NoResult, ServiceResult}

import scala.concurrent.Future

trait ContentService {

  def getContent(path: String): Future[ServiceResult[WebContent]]

  def createContent(parentPath: String, form: CreateForm, user: User): Future[ServiceResult[String]]

  def editContent(path: String, form: UpdateForm, user: User): Future[ServiceResult[NoResult]]

  def deleteContent(path: String, user: User): Future[ServiceResult[NoResult]]

}
