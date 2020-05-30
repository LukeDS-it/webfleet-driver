package it.ldsoftware.webfleet.driver.service

import it.ldsoftware.webfleet.driver.read.model.ContentRM

import scala.concurrent.Future

trait ContentReadService {
  def insertContent(rm: ContentRM): Future[ContentRM]

  def editContent(id: String, title: Option[String], desc: Option[String]): Future[Int]

  def deleteContent(id: String): Future[Int]
}
