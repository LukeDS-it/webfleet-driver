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
  def addContent(aggregate: String, content: Content, jwt: String): Either[DriverError, String]

  /**
    * Sets a content as published. If the user does not have the proper permission to publish the content, then
    * the content is marked as pending review from a moderator.
    *
    * @param name the name of the content to publish
    * @return either a DriverError if there is an error or Unit if successful
    */
  def publishContent(name: String, jwt: String): Either[DriverError, Unit]

  /**
    * Edits a content
    *
    * @param name    unique id of the content
    * @param content details of the content that must be edited
    * @return either a DriverError if there is an error or Unit if successful
    */
  def editContent(name: String, content: Content, jwt: String): Either[DriverError, Unit]

  /**
    * The review of a content is basically the same as editing, but checks different permissions on the
    * user requiring the operation.
    *
    * @param name    unique id of the content
    * @param content details of the content that must be edited
    * @param jwt     the jwt token with the auth information
    * @return either a DriverError if there is an error or Unit if successful
    */
  def reviewContent(name: String, content: Content, jwt: String): Either[DriverError, Unit]

  /**
    * Deletes a content
    *
    * @param name unique id of the content to remove
    * @param jwt  the jwt token with the auth information
    * @return either a DriverError if there is an error or Unit if successful
    */
  def deleteContent(name: String, jwt: String): Either[DriverError, Unit]

  /**
    * Moves a content from its current aggregate to another aggregate
    *
    * @param name the name of the content to move
    * @param to   name of the destination aggregate
    * @param jwt  the jwt token with the auth information
    * @return either a DriverError if there is an error or Unit if successful
    */
  def moveContent(name: String, to: String, jwt: String): Either[DriverError, Unit]

  /**
    * Sets the content as rejected, so it can't be published, providing information on why
    * the content has been rejected so the user can either delete the update or review it.
    *
    * @param name   the name of the content
    * @param reason reason of the rejection
    * @param jwt    the jwt token with the auth information
    * @return either a DriverError if there is an error or Unit if successful
    */
  def rejectContent(name: String, reason: String, jwt: String): Either[DriverError, Unit]

  /**
    * Sets the content as approved, with side effect of publishing it
    *
    * @param name the name of the content
    * @param jwt  the jwt token with the auth information
    * @return either a DriverError if there is an error or Unit if successful
    */
  def approveContent(name: String, jwt: String): Either[DriverError, Unit]

}
