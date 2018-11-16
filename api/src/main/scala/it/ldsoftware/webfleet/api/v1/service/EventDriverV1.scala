package it.ldsoftware.webfleet.api.v1.service

import java.time.LocalDateTime

import it.ldsoftware.webfleet.api.v1.model.{DriverError, Event}

trait EventDriverV1 {

  /**
    * Adds an event to the website.
    *
    * @param aggregate the aggregate's id where to insert the content
    * @param event     the event that must be created
    * @param jwt       the jwt token with the auth information
    * @return either a DriverError if there is an error or the unique identifier of the event created
    */
  def addEvent(aggregate: String, event: Event, jwt: String): Either[DriverError, String]

  /**
    * Edits an event
    *
    * @param name  the id of the event to be edited
    * @param event the event content to be edited
    * @param jwt   the jwt token with the auth information
    * @return either a DriverError if there is an error or Unit if successful
    */
  def editEvent(name: String, event: Event, jwt: String): Either[DriverError, Unit]

  /**
    * Reschedules an event
    *
    * @param name     the id of the event to reschedule
    * @param newStart if set, changes the start date of the event
    * @param newEnd   if set, changes the end date of the event
    * @param jwt      the jwt token with the auth information
    * @return either a DriverError if there is an error or Unit if successful
    */
  def rescheduleEvent(name: String,
                      newStart: Option[LocalDateTime] = None,
                      newEnd: Option[LocalDateTime] = None,
                      jwt: String): Either[DriverError, Unit]

  /**
    * Moves an event under another aggregate
    *
    * @param name the id of the event to move
    * @param to   the destination aggregate's id
    * @param jwt  the jwt token with the auth information
    * @return either a DriverError if there is an error or Unit if succesful
    */
  def moveEvent(name: String, to: String, jwt: String): Either[DriverError, Unit]

  /**
    * Marks the event as cancelled. Note: events can't be deleted, as this would be wrong from the point of view
    * of the users who first see an event and then they can't see it anymore.
    *
    * @param name   the name of the event
    * @param reason the reason why the event has been canceled (mandatory)
    * @param jwt    the jwt token with the auth information
    * @return
    */
  def cancelEvent(name: String, reason: String, jwt: String): Either[DriverError, Unit]
}
