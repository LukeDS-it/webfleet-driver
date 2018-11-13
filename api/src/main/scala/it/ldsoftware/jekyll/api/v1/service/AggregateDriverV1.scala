package it.ldsoftware.jekyll.api.v1.service

import it.ldsoftware.jekyll.api.v1.model._

/**
  * This trait defines operations that can be executed on aggregates by the Jekyll Driver service in its v1
  *
  * Client side implementation is left to application in order to leave the freedom of choice
  * on http frameworks.
  *
  * @author Luca Di Stefano
  */
trait AggregateDriverV1 {

  /**
    * Adds an aggregate to the website. An aggregate groups several types of elements in order to
    * give them a hierarchical structure. An aggregate can contain:
    *
    * - Other aggregates
    * - Events
    * - Contents
    * - Updates
    *
    * @param aggregate the aggregate that must be created
    * @return either a DriverError if it was not possible to insert the aggregate, or the unique identifier
    *         of the aggregate if the operation is successful
    */
  def addAggregate(aggregate: Aggregate): Either[DriverError, String]

  /**
    * Adds an aggregate to the website, inserting it inside another aggregate.
    *
    * @param parentAggregate the aggregate's id where to insert the content. If empty string,
    *                        then the first level is used
    * @param aggregate       the aggregate that must be created
    * @return either a DriverError if it was not possible to insert the aggregate, or the unique identifier
    *         of the aggregate if the operation is successful
    */
  def addAggregate(parentAggregate: String, aggregate: Aggregate): Either[DriverError, String]

  /**
    * Edits an aggregate
    *
    * @param name      the id of the aggregate that must be edited
    * @param aggregate the data of the aggregate that must be edited. Only data with Some(x) will be edited.
    * @return either a DriverError if there is an error or Unit if successful
    */
  def editAggregate(name: String, aggregate: Aggregate): Either[DriverError, Unit]

  /**
    * Deletes an aggregate and all of its content.
    *
    * @param name the id of the aggregate that must be deleted
    * @return either a DriverError if there is an error or Unit if successful
    */
  def deleteAggregate(name: String): Either[DriverError, Unit]

  /**
    * Moves an aggregate from its current parent aggregate to another aggregate.
    *
    * @param name        the name of the aggregate to move
    * @param destination the destination aggregate
    * @return either a DriverError if there is an error or Unit if successful
    */
  def moveAggregate(name: String, destination: String): Either[DriverError, Unit]

}
