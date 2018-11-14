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
    * Adds an aggregate to the website, inserting it inside another aggregate.
    *
    * @param parentAggregate the aggregate's id where to insert the content. If None,
    *                        then the first level is used
    * @param aggregate       the aggregate that must be created
    * @param jwt       the jwt token with the auth information
    * @return either a DriverError if it was not possible to insert the aggregate, or the unique identifier
    *         of the aggregate if the operation is successful
    */
  def addAggregate(parentAggregate: Option[String], aggregate: Aggregate, jwt: String): Either[DriverError, String]

  /**
    * Edits an aggregate
    *
    * @param name      the id of the aggregate that must be edited
    * @param aggregate the data of the aggregate that must be edited. Only data with Some(x) will be edited.
    * @param jwt       the jwt token with the auth information
    * @return either a DriverError if there is an error or Unit if successful
    */
  def editAggregate(name: String, aggregate: Aggregate, jwt: String): Either[DriverError, Unit]

  /**
    * Deletes an aggregate and all of its content.
    *
    * @param name the id of the aggregate that must be deleted
    * @param jwt       the jwt token with the auth information
    * @return either a DriverError if there is an error or Unit if successful
    */
  def deleteAggregate(name: String, jwt: String): Either[DriverError, Unit]

  /**
    * Moves an aggregate from its current parent aggregate to another aggregate.
    *
    * @param name        the name of the aggregate to move
    * @param destination the destination aggregate
    * @param jwt       the jwt token with the auth information
    * @return either a DriverError if there is an error or Unit if successful
    */
  def moveAggregate(name: String, destination: String, jwt: String): Either[DriverError, Unit]

}
