package it.ldsoftware.webfleet.driver.services.v1

import java.util.concurrent.CompletableFuture

import it.ldsoftware.webfleet.api.v1.events.AggregateEvent
import it.ldsoftware.webfleet.api.v1.events.AggregateEvent.{AddAggregate, EditAggregate}
import it.ldsoftware.webfleet.api.v1.model._
import it.ldsoftware.webfleet.driver.services.repositories.AggregateRepository
import it.ldsoftware.webfleet.driver.services.utils.EventUtils._
import it.ldsoftware.webfleet.driver.services.utils.TestUtils
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord, RecordMetadata}
import org.apache.kafka.common.TopicPartition
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Matchers, WordSpec}

class AggregateServiceSpec extends WordSpec with Matchers with MockitoSugar {

  val testAggregate = Aggregate(Some("name"), Some("description"), Some("text"))

  "The addAggregate function" should {
    "Return the name of the added aggregate when the execution succeeds" in {
      val producer = mock[KafkaProducer[String, String]]
      val repo = mock[AggregateRepository]

      val expectedRecord =
        new ProducerRecord[String, String](
          AggregateService.TopicName,
          "name",
          AggregateEvent(AddAggregate, Some(testAggregate)).toJsonString
        )
      val metadata = new RecordMetadata(new TopicPartition(AggregateService.TopicName, 0), 0L, 0L, 0L, 0L, 0, 0)

      doNothing().when(repo).addAggregate(None, testAggregate)
      when(producer.send(expectedRecord)).thenReturn(CompletableFuture.completedFuture(metadata))

      val subject = new AggregateService(producer, repo)

      subject.addAggregate(None, testAggregate, TestUtils.ValidJwt) shouldBe Created("name")

      verify(repo).addAggregate(None, testAggregate)
      verify(producer).send(expectedRecord)
    }

    "Correctly call the function to insert a child aggregate" in {
      val producer = mock[KafkaProducer[String, String]]
      val repo = mock[AggregateRepository]

      val expectedRecord =
        new ProducerRecord[String, String](
          AggregateService.TopicName,
          "name",
          AggregateEvent(AddAggregate, Some(testAggregate)).toJsonString
        )
      val metadata = new RecordMetadata(new TopicPartition(AggregateService.TopicName, 0), 0L, 0L, 0L, 0L, 0, 0)

      doNothing().when(repo).addAggregate(Some("parent"), testAggregate)
      when(repo.existsByName("parent")).thenReturn(true)
      when(producer.send(expectedRecord)).thenReturn(CompletableFuture.completedFuture(metadata))

      val subject = new AggregateService(producer, repo)

      subject.addAggregate(Some("parent"), testAggregate, TestUtils.ValidJwt) shouldBe Created("name")

      verify(repo).addAggregate(Some("parent"), testAggregate)
      verify(producer).send(expectedRecord)
    }

    "Return a validation error when some fields are not valid" in {
      val producer = mock[KafkaProducer[String, String]]
      val repo = mock[AggregateRepository]
      val invalidAggregate = Aggregate(None, None, None)

      doNothing().when(repo).addAggregate(None, invalidAggregate)

      val subject = new AggregateService(producer, repo)

      subject.addAggregate(None, invalidAggregate, TestUtils.ValidJwt) shouldBe ValidationError(
        Set(
          FieldError("name", "Aggregate name cannot be empty"),
          FieldError("text", "Aggregate text cannot be empty")
        )
      )

      verify(repo, never).addAggregate(None, invalidAggregate)
      verify(producer, never).send(any[ProducerRecord[String, String]])
    }

    "Return a validation error when an aggregate with same name already exists" in {
      val producer = mock[KafkaProducer[String, String]]
      val repo = mock[AggregateRepository]

      doNothing().when(repo).addAggregate(None, testAggregate)
      when(repo.existsByName(testAggregate.name.get)).thenReturn(true)

      val subject = new AggregateService(producer, repo)

      subject.addAggregate(None, testAggregate, TestUtils.ValidJwt) shouldBe ValidationError(
        Set(
          FieldError("name", "Aggregate with same name already exists")
        )
      )

      verify(repo, never).addAggregate(None, testAggregate)
      verify(producer, never).send(any[ProducerRecord[String, String]])
    }

    "Return a validation error when trying to insert an aggregate under a non existing aggregate" in {
      val producer = mock[KafkaProducer[String, String]]
      val repo = mock[AggregateRepository]

      doNothing().when(repo).addAggregate(None, testAggregate)
      when(repo.existsByName("parent")).thenReturn(false)

      val subject = new AggregateService(producer, repo)

      subject.addAggregate(Some("parent"), testAggregate, TestUtils.ValidJwt) shouldBe ValidationError(
        Set(
          FieldError("parent", "Specified parent does not exist")
        )
      )

      verify(repo, never).addAggregate(None, testAggregate)
      verify(producer, never).send(any[ProducerRecord[String, String]])
    }

    "Return the failure from the database when the database can't insert data" in {
      val producer = mock[KafkaProducer[String, String]]
      val repo = mock[AggregateRepository]

      when(repo.addAggregate(None, testAggregate)).thenThrow(new RuntimeException("Something went wrong in pgsql"))

      val subject = new AggregateService(producer, repo)

      subject.addAggregate(None, testAggregate, TestUtils.ValidJwt) shouldBe ServerError("Something went wrong in pgsql")

      verify(repo).addAggregate(None, testAggregate)
      verify(producer, never).send(any[ProducerRecord[String, String]])
    }

    "Return the failure from kafka when data can't be sent to kafka" in {
      val producer = mock[KafkaProducer[String, String]]
      val repo = mock[AggregateRepository]

      val expectedRecord =
        new ProducerRecord[String, String](
          AggregateService.TopicName,
          "name",
          AggregateEvent(AddAggregate, Some(testAggregate)).toJsonString
        )

      doNothing().when(repo).addAggregate(None, testAggregate)
      when(producer.send(expectedRecord)).thenThrow(new RuntimeException("Something went wrong in kafka"))

      val subject = new AggregateService(producer, repo)

      subject.addAggregate(None, testAggregate, TestUtils.ValidJwt) shouldBe ServerError("Something went wrong in kafka")

      verify(repo).addAggregate(None, testAggregate)
      verify(producer).send(expectedRecord)
    }
  }

  "The editAggregate function" should {
    "Update just the edited fields" in {
      val producer = mock[KafkaProducer[String, String]]
      val repo = mock[AggregateRepository]

      val editData = Aggregate(None, None, Some("New Text"))
      val edited = testAggregate.copy(
        text = Some("New Text")
      )
      val evt = AggregateEvent(EditAggregate, Some(edited)).toJsonString

      val expectedRecord = new ProducerRecord[String, String](AggregateService.TopicName, testAggregate.name.get, evt)
      val metadata = new RecordMetadata(new TopicPartition(AggregateService.TopicName, 0), 0L, 0L, 0L, 0L, 0, 0)

      doNothing().when(repo).updateAggregate(testAggregate.name.get, edited)
      when(repo.existsByName(testAggregate.name.get)).thenReturn(true)
      when(repo.getAggregate(testAggregate.name.get)).thenReturn(Some(testAggregate))
      when(producer.send(expectedRecord)).thenReturn(CompletableFuture.completedFuture(metadata))

      val subject = new AggregateService(producer, repo)

      subject.editAggregate(testAggregate.name.get, editData, TestUtils.ValidJwt) shouldBe NoContent

      verify(repo).updateAggregate(testAggregate.name.get, edited)
      verify(producer).send(expectedRecord)
    }

    "Insert a new aggregate if it doesn't already exist on db" in {
      val producer = mock[KafkaProducer[String, String]]
      val repo = mock[AggregateRepository]

      val expectedRecord =
        new ProducerRecord[String, String](
          AggregateService.TopicName,
          "name",
          AggregateEvent(AddAggregate, Some(testAggregate)).toJsonString
        )
      val metadata = new RecordMetadata(new TopicPartition(AggregateService.TopicName, 0), 0L, 0L, 0L, 0L, 0, 0)

      doNothing().when(repo).addAggregate(None, testAggregate)
      when(producer.send(expectedRecord)).thenReturn(CompletableFuture.completedFuture(metadata))

      val subject = new AggregateService(producer, repo)

      subject.editAggregate(testAggregate.name.get, testAggregate, TestUtils.ValidJwt) shouldBe Created("name")

      verify(repo).addAggregate(None, testAggregate)
      verify(producer).send(expectedRecord)
    }

    "Return a validation error if trying to edit text with empty text" in {
      val producer = mock[KafkaProducer[String, String]]
      val repo = mock[AggregateRepository]

      val editData = Aggregate(None, None, Some(""))

      when(repo.existsByName(testAggregate.name.get)).thenReturn(true)
      when(repo.getAggregate(testAggregate.name.get)).thenReturn(Some(testAggregate))

      val subject = new AggregateService(producer, repo)

      subject.editAggregate(testAggregate.name.get, editData, TestUtils.ValidJwt) shouldBe ValidationError(
        Set(
          FieldError("text", "Aggregate text cannot be empty")
        )
      )

      verify(repo, never).updateAggregate(any[String], any[Aggregate])
      verify(producer, never).send(any[ProducerRecord[String, String]])
    }

    "Return a validation error if trying to change name with a name that already exists" in {
      val producer = mock[KafkaProducer[String, String]]
      val repo = mock[AggregateRepository]

      val editData = Aggregate(Some("newName"), None, None)

      when(repo.existsByName(testAggregate.name.get)).thenReturn(true)
      when(repo.existsByName("newName")).thenReturn(true)
      when(repo.getAggregate(testAggregate.name.get)).thenReturn(Some(testAggregate))

      val subject = new AggregateService(producer, repo)

      subject.editAggregate(testAggregate.name.get, editData, TestUtils.ValidJwt) shouldBe ValidationError(
        Set(
          FieldError("name", "Aggregate with same name already exists")
        )
      )

      verify(repo, never).updateAggregate(any[String], any[Aggregate])
      verify(producer, never).send(any[ProducerRecord[String, String]])
    }
  }

}
