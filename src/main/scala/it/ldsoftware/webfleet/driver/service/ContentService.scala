package it.ldsoftware.webfleet.driver.service

import it.ldsoftware.webfleet.driver.actors.model.{CreateForm, UpdateForm, WebContent}
import it.ldsoftware.webfleet.driver.security.User
import it.ldsoftware.webfleet.driver.service.model.{NoResult, ServiceResult}

import scala.concurrent.Future

trait ContentService {

  def getContent(domain: String, path: String): Future[ServiceResult[WebContent]]

  def createContent(domain: String, path: String, form: CreateForm, user: User): Future[ServiceResult[String]]

  def editContent(domain: String, path: String, form: UpdateForm, user: User): Future[ServiceResult[NoResult]]

  def deleteContent(domain: String, path: String, user: User): Future[ServiceResult[NoResult]]

}
