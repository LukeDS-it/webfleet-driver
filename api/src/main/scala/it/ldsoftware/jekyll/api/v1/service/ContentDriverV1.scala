package it.ldsoftware.jekyll.api.v1.service

import it.ldsoftware.jekyll.api.v1.model.{Content, DriverError}

trait ContentDriverV1 {
  /**
    * Adds a content (static page) to the website
    *
    * @param aggregate the aggregate's id where to insert the content
    * @param content   the content that must be created
    * @return either a DriverError if it was not possible to insert the page, or the unique identifier of the page if
    *         the operation is successful
    */
  def addContent(aggregate: String, content: Content): Either[DriverError, String]

  /**
    * Sets a content as published
    *
    * @param name the name of the content to publish
    * @return either a DriverError if there is an error or Unit if successful
    */
  def publishContent(name: String): Either[DriverError, Unit]

  /**
    * Edits a content
    *
    * @param name    unique id of the content
    * @param content details of the content that must be edited
    * @return either a DriverError if there is an error or Unit if successful
    */
  def editContent(name: String, content: Content): Either[DriverError, Unit]

  /**
    * The review of a content is basically the same as editing, but checks different permissions on the
    * user requiring the operation.
    *
    * @param name    unique id of the content
    * @param content details of the content that must be edited
    * @return either a DriverError if there is an error or Unit if successful
    */
  def reviewContent(name: String, content: Content): Either[DriverError, Unit]

  /**
    * Deletes a content
    *
    * @param name unique id of the content to remove
    * @return either a DriverError if there is an error or Unit if successful
    */
  def deleteContent(name: String): Either[DriverError, Unit]

  /**
    * Moves a content from its current aggregate to another aggregate
    *
    * @param name the name of the content to move
    * @param to   name of the destination aggregate
    * @return either a DriverError if there is an error or Unit if successful
    */
  def moveContent(name: String, to: String): Either[DriverError, Unit]

  /**
    * Sets the content as rejected, so it can't be published
    *
    * @param name   the name of the content
    * @param reason reason of the rejection
    * @return either a DriverError if there is an error or Unit if successful
    */
  def rejectContent(name: String, reason: String): Either[DriverError, Unit]

  /**
    * Sets the content as approved, with side effect of publishing it
    *
    * @param name the name of the content
    * @return either a DriverError if there is an error or Unit if successful
    */
  def approveContent(name: String): Either[DriverError, Unit]

}
