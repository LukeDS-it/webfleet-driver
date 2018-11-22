package it.ldsoftware.webfleet.driver.services.repositories

import it.ldsoftware.webfleet.api.v1.model.Aggregate
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, WordSpec}
import scalikejdbc._
import AggregateRepository._

class AggregateRepositorySpec extends WordSpec with Matchers with BeforeAndAfterEach with BeforeAndAfterAll {

  Class.forName("org.h2.Driver")
  ConnectionPool.singleton("jdbc:h2:mem:default", "sa", "sa")

  implicit val session: DBSession = AutoSession

  val testName = "name"
  val testDesc = "desc"
  val testText = "text"
  val testPart = "part"

  val expectedName = "agN"
  val expectedDesc = "agD"
  val expectedText = "agT"
  val expectedPart = "agP"

  val aggregate = Aggregate(Some(expectedName), Some(expectedDesc), Some(expectedText))

  val subject = new AggregateRepository()

  override def beforeAll(): Unit = {
    sql"create table aggregate (name varchar(255), description varchar(255), text text, parent varchar(255))"
      .execute
      .apply()
  }

  override def afterAll(): Unit = sql"drop table aggregate".execute.apply()

  override def beforeEach(): Unit = {
    sql"truncate table aggregate".execute.apply()
    sql"insert into aggregate(name, description, text, parent) values ($testName, $testDesc, $testText, $testPart)"
      .execute
      .apply()
  }

  "The existsByName function" should {
    "correctly return true if an element exists" in {
      subject.existsByName(testName) shouldBe true
    }

    "correctly return false if an element does not exist" in {
      subject.existsByName("none") shouldBe false
    }
  }

  "The getAggregate function" should {
    "correctly get an aggregate" in {
      subject.getAggregate(testName) shouldBe Some(Aggregate(Some(testName), Some(testDesc), Some(testText)))
    }
  }

  "The addAggregate function" should {
    "correctly insert an aggregate with no parent" in {
      subject.addAggregate(None, aggregate)

      subject.existsByName(expectedName) shouldBe true
      subject.getAggregate(expectedName) shouldBe Some(aggregate)
      sql"select parent from aggregate where name = $expectedName"
        .map(_.string(ColumnParent))
        .single
        .apply() shouldBe None
    }

    "correctly insert an aggregate with a parent" in {
      subject.addAggregate(Some(expectedPart), aggregate)

      subject.existsByName(expectedName) shouldBe true
      subject.getAggregate(expectedName) shouldBe Some(aggregate)
      sql"select parent from aggregate where name = $expectedName"
        .map(_.string(ColumnParent))
        .single
        .apply() shouldBe Some(expectedPart)
    }
  }

  "The listAggregates function" should {
    "recover two elements" in {
      subject.addAggregate(None, aggregate)

      val result = subject.listAggregates(0, 2)
      result should have size 2
      result.sortBy(_.name) shouldBe List(aggregate, Aggregate(Some(testName), Some(testDesc), Some(testText)))
    }

    "recover one element in the first page" in {
      subject.addAggregate(None, aggregate)

      val result = subject.listAggregates(0, 1)
      result should have size 1
      result shouldBe List(Aggregate(Some(testName), Some(testDesc), Some(testText)))
    }

    "recover one element in the second page" in {
      subject.addAggregate(None, aggregate)

      val result = subject.listAggregates(1, 1)
      result should have size 1
      result shouldBe List(aggregate)
    }
  }


}
