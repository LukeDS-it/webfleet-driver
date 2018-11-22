package it.ldsoftware.webfleet.driver.services.repositories

import it.ldsoftware.webfleet.api.v1.model.Aggregate
import scalikejdbc._
import AggregateRepository._

class AggregateRepository {

  implicit val session: DBSession = AutoSession

  def existsByName(name: String): Boolean =
    sql"select count(*) as cnt from aggregate where name = $name"
      .map(rs => rs.int("cnt") > 0)
      .single()
      .apply().get


  def addAggregate(parent: Option[String], agg: Aggregate): Unit =
    sql"""insert into aggregate(name, description, text, parent)
          values (${agg.name}, ${agg.description}, ${agg.text}, $parent)"""
      .update().apply()

  def getAggregate(name: String): Option[Aggregate] =
    sql"select * from aggregate where name = $name"
      .map(rs => Aggregate(Some(rs.string(ColumnName)), Some(rs.string(ColumnDescription)), Some(rs.string(ColumnText))))
      .single
      .apply()

  def listAggregates(page: Int, size: Int): List[Aggregate] =
    sql"select * from aggregate limit $size offset $page"
      .map(rs => Aggregate(Some(rs.string(ColumnName)), Some(rs.string(ColumnDescription)), Some(rs.string(ColumnText))))
      .list
      .apply

  def updateAggregate(name: String, agg: Aggregate): Unit =
    sql"update aggregate set name = ${agg.name}, description = ${agg.description}, text = ${agg.text} where name = $name"
      .update().apply()

  def deleteAggregate(name: String): Unit = sql"delete from aggregate where name = $name".execute.apply()

  def moveAggregate(name: String, to: String): Unit =
    sql"update aggregate set parent = $to where name = $name".update().apply()
}

object AggregateRepository {
  val ColumnName = "name"
  val ColumnDescription = "description"
  val ColumnText = "text"
  val ColumnParent = "parent"
}