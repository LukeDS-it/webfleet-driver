package it.ldsoftware.webfleet.api.v1.service

import it.ldsoftware.webfleet.api.v1.auth.Principal
import it.ldsoftware.webfleet.api.v1.model.{Content, DriverResult}

trait ContentDriverV1 {
  /**
    * Adds a content (static page) to the website
    *
    * @param aggregate the aggregate's id where to insert the content
    * @param content   the content that must be created
    * @return a DriverResult object for pattern matching
    */
  def addContent(aggregate: String, content: Content, principal: Principal): DriverResult

  /**
    * Sets a content as published. If the user does not have the proper permission to publish the content, then
    * the content is marked as pending review from a moderator.
    *
    * @param name      the name of the content to publish
    * @param principal the principal of the user performing the action
    * @return a DriverResult object for pattern matching
    */
  def publishContent(name: String, principal: Principal): DriverResult

  /**
    * Edits a content
    *
    * @param name    unique id of the content
    * @param content details of the content that must be edited
    * @return a DriverResult object for pattern matching
    */
  def editContent(name: String, content: Content, principal: Principal): DriverResult

  /**
    * The review of a content is basically the same as editing, but checks different permissions on the
    * user requiring the operation.
    *
    * @param name      unique id of the content
    * @param content   details of the content that must be edited
    * @param principal the principal of the user performing the action
    * @return a DriverResult object for pattern matching
    */
  def reviewContent(name: String, content: Content, principal: Principal): DriverResult

  /**
    * Deletes a content
    *
    * @param name      unique id of the content to remove
    * @param principal the principal of the user performing the action
    * @return a DriverResult object for pattern matching
    */
  def deleteContent(name: String, principal: Principal): DriverResult

  /**
    * Moves a content from its current aggregate to another aggregate
    *
    * @param name      the name of the content to move
    * @param to        name of the destination aggregate
    * @param principal the principal of the user performing the action
    * @return a DriverResult object for pattern matching
    */
  def moveContent(name: String, to: String, principal: Principal): DriverResult

  /**
    * Sets the content as rejected, so it can't be published, providing information on why
    * the content has been rejected so the user can either delete the update or review it.
    *
    * @param name      the name of the content
    * @param reason    reason of the rejection
    * @param principal the principal of the user performing the action
    * @return a DriverResult object for pattern matching
    */
  def rejectContent(name: String, reason: String, principal: Principal): DriverResult

  /**
    * Sets the content as approved, with side effect of publishing it
    *
    * @param name      the name of the content
    * @param principal the principal of the user performing the action
    * @return a DriverResult object for pattern matching
    */
  def approveContent(name: String, principal: Principal): DriverResult

}
