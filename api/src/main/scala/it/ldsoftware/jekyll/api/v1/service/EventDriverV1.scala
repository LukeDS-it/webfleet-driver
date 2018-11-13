package it.ldsoftware.jekyll.api.v1.service

import java.time.LocalDateTime

import it.ldsoftware.jekyll.api.v1.model.{DriverError, Event}

trait EventDriverV1 {

  /**
    * Adds an event to the website.
    *
    * @param aggregate the aggregate's id where to insert the content
    * @param event     the event that must be created
    */
  def addEvent(aggregate: String, event: Event): Either[DriverError, String]

  def editEvent(name: String, event: Event): Either[DriverError, Unit]

  def rescheduleEvent(name: String, newStart: Option[LocalDateTime], newEnd: Option[LocalDateTime]): Either[DriverError, Unit]

  def moveEvent(name: String, to: String): Either[DriverError, Unit]

  def cancelEvent(name: String): Either[DriverError, Unit]
}
