package it.ldsoftware.jekyll.api.v1.service

import it.ldsoftware.jekyll.api.v1.model.{DriverError, Update}

trait UpdateDriverV1 {
  /**
    * Adds an update (blog entry) to the website.
    *
    * @param aggregate the aggregate's id where to insert the content
    * @param update    the update that must be created
    */
  def addUpdate(aggregate: String, update: Update): Either[DriverError, String]

  def publishUpdate(name: String): Either[DriverError, Unit]

  def editUpdate(name: String, update: Update): Either[DriverError, Unit]

  def reviewUpdate(name: String, update: Update): Either[DriverError, Unit]

  def deleteUpdate(name: String): Either[DriverError, Unit]

  def rejectUpdate(name: String, reason: String): Either[DriverError, Unit]

  def approveUpdate(name: String): Either[DriverError, Unit]
}
