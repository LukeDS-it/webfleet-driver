package it.ldsoftware.webfleet.api.v1.service

import java.time.LocalDateTime

import it.ldsoftware.webfleet.api.v1.auth.Principal
import it.ldsoftware.webfleet.api.v1.model.{DriverResult, Event}

trait EventDriverV1 {

  /**
    * Adds an event to the website.
    *
    * @param aggregate the aggregate's id where to insert the content
    * @param event     the event that must be created
    * @param principal the principal of the user performing the action
    * @return a DriverResult object for pattern matching
    */
  def addEvent(aggregate: String, event: Event, principal: Principal): DriverResult

  /**
    * Edits an event
    *
    * @param name      the id of the event to be edited
    * @param event     the event content to be edited
    * @param principal the principal of the user performing the action
    * @return a DriverResult object for pattern matching
    */
  def editEvent(name: String, event: Event, principal: Principal): DriverResult

  /**
    * Reschedules an event
    *
    * @param name      the id of the event to reschedule
    * @param newStart  if set, changes the start date of the event
    * @param newEnd    if set, changes the end date of the event
    * @param principal the principal of the user performing the action
    * @return a DriverResult object for pattern matching
    */
  def rescheduleEvent(name: String,
                      newStart: Option[LocalDateTime] = None,
                      newEnd: Option[LocalDateTime] = None,
                      principal: Principal): DriverResult

  /**
    * Moves an event under another aggregate
    *
    * @param name      the id of the event to move
    * @param to        the destination aggregate's id
    * @param principal the principal of the user performing the action
    * @return a DriverResult object for pattern matching
    */
  def moveEvent(name: String, to: String, principal: Principal): DriverResult

  /**
    * Marks the event as cancelled. Note: events can't be deleted, as this would be wrong from the point of view
    * of the users who first see an event and then they can't see it anymore.
    *
    * @param name      the name of the event
    * @param reason    the reason why the event has been canceled (mandatory)
    * @param principal the principal of the user performing the action
    * @return a DriverResult object for pattern matching
    */
  def cancelEvent(name: String, reason: String, principal: Principal): DriverResult
}
