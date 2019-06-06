package it.ldsoftware.webfleet.driver.services.v1

import java.util.concurrent.CompletableFuture

import it.ldsoftware.webfleet.api.v1.auth.Principal
import it.ldsoftware.webfleet.api.v1.events.AggregateEvent
import it.ldsoftware.webfleet.api.v1.events.AggregateEvent.{AddAggregate, DeleteAggregate, EditAggregate, MoveAggregate}
import it.ldsoftware.webfleet.api.v1.model._
import it.ldsoftware.webfleet.driver.routes.utils.PrincipalExtractor
import it.ldsoftware.webfleet.driver.services.repositories.AggregateRepository
import it.ldsoftware.webfleet.driver.services.utils.AuthenticationUtils
import it.ldsoftware.webfleet.driver.services.utils.EventUtils._
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
      val fakePrincipal = Principal("name", Set(AuthenticationUtils.ScopeAddAggregate))

      val subject = new AggregateService(producer, repo)

      subject.addAggregate(None, testAggregate, fakePrincipal) shouldBe Created("name")

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
      val fakePrincipal = Principal("name", Set(AuthenticationUtils.ScopeAddAggregate))

      val subject = new AggregateService(producer, repo)

      subject.addAggregate(Some("parent"), testAggregate, fakePrincipal) shouldBe Created("name")

      verify(repo).addAggregate(Some("parent"), testAggregate)
      verify(producer).send(expectedRecord)
    }

    "Return a validation error when some fields are not valid" in {
      val producer = mock[KafkaProducer[String, String]]
      val repo = mock[AggregateRepository]

      val invalidAggregate = Aggregate(None, None, None)

      doNothing().when(repo).addAggregate(None, invalidAggregate)
      val fakePrincipal = Principal("name", Set(AuthenticationUtils.ScopeAddAggregate))

      val subject = new AggregateService(producer, repo)

      subject.addAggregate(None, invalidAggregate, fakePrincipal) shouldBe ValidationError(
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
      val fakePrincipal = Principal("name", Set(AuthenticationUtils.ScopeAddAggregate))

      val subject = new AggregateService(producer, repo)

      subject.addAggregate(None, testAggregate, fakePrincipal) shouldBe ValidationError(
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
      val fakePrincipal = Principal("name", Set(AuthenticationUtils.ScopeAddAggregate))

      val subject = new AggregateService(producer, repo)

      subject.addAggregate(Some("parent"), testAggregate, fakePrincipal) shouldBe ValidationError(
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
      val fakePrincipal = Principal("name", Set(AuthenticationUtils.ScopeAddAggregate))

      val subject = new AggregateService(producer, repo)

      subject.addAggregate(None, testAggregate, fakePrincipal) shouldBe ServerError("Something went wrong in pgsql")

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
      val fakePrincipal = Principal("name", Set(AuthenticationUtils.ScopeAddAggregate))

      val subject = new AggregateService(producer, repo)

      subject.addAggregate(None, testAggregate, fakePrincipal) shouldBe ServerError("Something went wrong in kafka")

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
      val fakePrincipal = Principal("name", Set(AuthenticationUtils.ScopeEditAggregate))

      val subject = new AggregateService(producer, repo)

      subject.editAggregate(testAggregate.name.get, editData, fakePrincipal) shouldBe NoContent

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
      val fakePrincipal = Principal("name", Set(AuthenticationUtils.ScopeEditAggregate, AuthenticationUtils.ScopeAddAggregate))

      val subject = new AggregateService(producer, repo)

      subject.editAggregate(testAggregate.name.get, testAggregate, fakePrincipal) shouldBe Created("name")

      verify(repo).addAggregate(None, testAggregate)
      verify(producer).send(expectedRecord)
    }

    "Return a validation error if trying to edit text with empty text" in {
      val producer = mock[KafkaProducer[String, String]]
      val repo = mock[AggregateRepository]

      val editData = Aggregate(None, None, Some(""))

      when(repo.existsByName(testAggregate.name.get)).thenReturn(true)
      when(repo.getAggregate(testAggregate.name.get)).thenReturn(Some(testAggregate))
      val fakePrincipal = Principal("name", Set(AuthenticationUtils.ScopeEditAggregate))

      val subject = new AggregateService(producer, repo)

      subject.editAggregate(testAggregate.name.get, editData, fakePrincipal) shouldBe ValidationError(
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
      val fakePrincipal = Principal("name", Set(AuthenticationUtils.ScopeEditAggregate))

      val subject = new AggregateService(producer, repo)

      subject.editAggregate(testAggregate.name.get, editData, fakePrincipal) shouldBe ValidationError(
        Set(
          FieldError("name", "Aggregate with same name already exists")
        )
      )

      verify(repo, never).updateAggregate(any[String], any[Aggregate])
      verify(producer, never).send(any[ProducerRecord[String, String]])
    }

    "Return the failure from the database when the database can't insert data" in {
      val producer = mock[KafkaProducer[String, String]]
      val repo = mock[AggregateRepository]

      when(repo.existsByName(testAggregate.name.get)).thenReturn(true)
      when(repo.getAggregate(testAggregate.name.get)).thenReturn(Some(testAggregate))
      when(repo.updateAggregate(testAggregate.name.get, testAggregate))
        .thenThrow(new RuntimeException("Something went wrong in pgsql"))
      val fakePrincipal = Principal("name", Set(AuthenticationUtils.ScopeEditAggregate))

      val subject = new AggregateService(producer, repo)
      val edited = Aggregate(None, None, testAggregate.text)

      subject
        .editAggregate(testAggregate.name.get, edited, fakePrincipal)
        .shouldBe(ServerError("Something went wrong in pgsql"))

      verify(repo).updateAggregate(testAggregate.name.get, testAggregate)
      verify(producer, never).send(any[ProducerRecord[String, String]])
    }

    "Return the failure from kafka when data can't be sent to kafka" in {
      val producer = mock[KafkaProducer[String, String]]
      val repo = mock[AggregateRepository]

      val expectedRecord =
        new ProducerRecord[String, String](
          AggregateService.TopicName,
          "name",
          AggregateEvent(EditAggregate, Some(testAggregate)).toJsonString
        )

      when(repo.existsByName(testAggregate.name.get)).thenReturn(true)
      when(repo.getAggregate(testAggregate.name.get)).thenReturn(Some(testAggregate))
      doNothing().when(repo).updateAggregate(testAggregate.name.get, testAggregate)
      when(producer.send(expectedRecord)).thenThrow(new RuntimeException("Something went wrong in kafka"))
      val fakePrincipal = Principal("name", Set(AuthenticationUtils.ScopeEditAggregate))

      val subject = new AggregateService(producer, repo)

      subject
        .editAggregate(testAggregate.name.get, testAggregate, fakePrincipal)
        .shouldBe(ServerError("Something went wrong in kafka"))

      verify(repo).updateAggregate(testAggregate.name.get, testAggregate)
      verify(producer).send(expectedRecord)
    }
  }

  "The deleteAggregate function" should {
    "Correctly send the remove aggregate event and remove the aggregate from write side" in {
      val producer = mock[KafkaProducer[String, String]]
      val repo = mock[AggregateRepository]

      val deleted = Aggregate(testAggregate.name, None, None)
      val evt = AggregateEvent(DeleteAggregate, Some(deleted)).toJsonString
      val expectedRecord = new ProducerRecord[String, String](AggregateService.TopicName, testAggregate.name.get, evt)
      val metadata = new RecordMetadata(new TopicPartition(AggregateService.TopicName, 0), 0L, 0L, 0L, 0L, 0, 0)

      doNothing().when(repo).deleteAggregate(any[String])
      when(repo.existsByName(testAggregate.name.get)).thenReturn(true)
      when(producer.send(expectedRecord)).thenReturn(CompletableFuture.completedFuture(metadata))
      val fakePrincipal = Principal("name", Set(AuthenticationUtils.ScopeDeleteAggregate))

      val subject = new AggregateService(producer, repo)

      subject.deleteAggregate(testAggregate.name.get, fakePrincipal) shouldBe NoContent

      verify(repo).deleteAggregate(testAggregate.name.get)
      verify(producer).send(expectedRecord)
    }

    "Return not found when trying to delete an aggregate that does not exist" in {
      val producer = mock[KafkaProducer[String, String]]
      val repo = mock[AggregateRepository]

      when(repo.existsByName(testAggregate.name.get)).thenReturn(false)
      val fakePrincipal = Principal("name", Set(AuthenticationUtils.ScopeDeleteAggregate))

      val subject = new AggregateService(producer, repo)

      subject.deleteAggregate(testAggregate.name.get, fakePrincipal) shouldBe NotFoundError

      verify(repo, never).deleteAggregate(testAggregate.name.get)
      verify(producer, never).send(any[ProducerRecord[String, String]])
    }

    "Return the failure from the database when the database can't delete data" in {
      val producer = mock[KafkaProducer[String, String]]
      val repo = mock[AggregateRepository]

      when(repo.existsByName(testAggregate.name.get)).thenReturn(true)
      when(repo.getAggregate(testAggregate.name.get)).thenReturn(Some(testAggregate))
      when(repo.deleteAggregate(testAggregate.name.get))
        .thenThrow(new RuntimeException("Something went wrong in pgsql"))
      val fakePrincipal = Principal("name", Set(AuthenticationUtils.ScopeDeleteAggregate))

      val subject = new AggregateService(producer, repo)

      subject
        .deleteAggregate(testAggregate.name.get, fakePrincipal)
        .shouldBe(ServerError("Something went wrong in pgsql"))

      verify(repo).deleteAggregate(testAggregate.name.get)
      verify(producer, never).send(any[ProducerRecord[String, String]])
    }

    "Return the failure from kafka when data can't be sent to kafka" in {
      val producer = mock[KafkaProducer[String, String]]
      val repo = mock[AggregateRepository]

      val expectedRecord =
        new ProducerRecord[String, String](
          AggregateService.TopicName,
          "name",
          AggregateEvent(DeleteAggregate, Some(Aggregate(testAggregate.name, None, None))).toJsonString
        )

      when(repo.existsByName(testAggregate.name.get)).thenReturn(true)
      when(repo.getAggregate(testAggregate.name.get)).thenReturn(Some(testAggregate))
      doNothing().when(repo).deleteAggregate(testAggregate.name.get)
      when(producer.send(expectedRecord)).thenThrow(new RuntimeException("Something went wrong in kafka"))
      val fakePrincipal = Principal("name", Set(AuthenticationUtils.ScopeDeleteAggregate))

      val subject = new AggregateService(producer, repo)

      subject
        .deleteAggregate(testAggregate.name.get, fakePrincipal)
        .shouldBe(ServerError("Something went wrong in kafka"))

      verify(repo).deleteAggregate(testAggregate.name.get)
      verify(producer).send(expectedRecord)
    }
  }

  "The moveAggregate function" should {
    "Correctly move an aggregate" in {
      val producer = mock[KafkaProducer[String, String]]
      val repo = mock[AggregateRepository]

      val evt = AggregateEvent(MoveAggregate, Some(Aggregate(Some("parent"), None, None))).toJsonString
      val expectedRecord = new ProducerRecord[String, String](AggregateService.TopicName, testAggregate.name.get, evt)
      val metadata = new RecordMetadata(new TopicPartition(AggregateService.TopicName, 0), 0L, 0L, 0L, 0L, 0, 0)

      when(repo.existsByName(testAggregate.name.get)).thenReturn(true)
      when(repo.existsByName("parent")).thenReturn(true)
      doNothing().when(repo).moveAggregate(testAggregate.name.get, "parent")
      when(producer.send(expectedRecord)).thenReturn(CompletableFuture.completedFuture(metadata))
      val fakePrincipal = Principal("name", Set(AuthenticationUtils.ScopeMoveAggregate))

      val subject = new AggregateService(producer, repo)

      subject.moveAggregate(testAggregate.name.get, "parent", fakePrincipal) shouldBe NoContent

      verify(repo).moveAggregate(testAggregate.name.get, "parent")
      verify(producer).send(expectedRecord)
    }

    "Return not found when trying to move an aggregate that does not exist" in {
      val producer = mock[KafkaProducer[String, String]]
      val repo = mock[AggregateRepository]

      when(repo.existsByName(testAggregate.name.get)).thenReturn(false)
      val fakePrincipal = Principal("name", Set(AuthenticationUtils.ScopeMoveAggregate))

      val subject = new AggregateService(producer, repo)

      subject.moveAggregate(testAggregate.name.get, "parent", fakePrincipal) shouldBe NotFoundError

      verify(repo, never).moveAggregate(testAggregate.name.get, "parent")
      verify(producer, never).send(any[ProducerRecord[String, String]])
    }

    "Return a validation error when trying to move to a non existing destination" in {
      val producer = mock[KafkaProducer[String, String]]
      val repo = mock[AggregateRepository]

      when(repo.existsByName(testAggregate.name.get)).thenReturn(true)
      when(repo.existsByName("parent")).thenReturn(false)
      val fakePrincipal = Principal("name", Set(AuthenticationUtils.ScopeMoveAggregate))

      val subject = new AggregateService(producer, repo)

      subject.moveAggregate(testAggregate.name.get, "parent", fakePrincipal) shouldBe ValidationError(
        Set(
          FieldError("destination", "Destination aggregate does not exist")
        )
      )
      verify(repo, never).moveAggregate(testAggregate.name.get, "parent")
      verify(producer, never).send(any[ProducerRecord[String, String]])
    }

    "Return a validation error when trying to move an aggregate into itself" in {
      val producer = mock[KafkaProducer[String, String]]
      val repo = mock[AggregateRepository]

      when(repo.existsByName(testAggregate.name.get)).thenReturn(true)
      val fakePrincipal = Principal("name", Set(AuthenticationUtils.ScopeMoveAggregate))

      val subject = new AggregateService(producer, repo)

      subject.moveAggregate(testAggregate.name.get, testAggregate.name.get, fakePrincipal) shouldBe ValidationError(
        Set(
          FieldError("destination", "An aggregate can't be moved into itself")
        )
      )
      verify(repo, never).moveAggregate(testAggregate.name.get, testAggregate.name.get)
      verify(producer, never).send(any[ProducerRecord[String, String]])
    }

    "Return the failure from the database when the database can't insert data" in {
      val producer = mock[KafkaProducer[String, String]]
      val repo = mock[AggregateRepository]

      when(repo.existsByName(testAggregate.name.get)).thenReturn(true)
      when(repo.existsByName("parent")).thenReturn(true)
      when(repo.getAggregate(testAggregate.name.get)).thenReturn(Some(testAggregate))
      when(repo.moveAggregate(testAggregate.name.get, "parent"))
        .thenThrow(new RuntimeException("Something went wrong in pgsql"))
      val fakePrincipal = Principal("name", Set(AuthenticationUtils.ScopeMoveAggregate))

      val subject = new AggregateService(producer, repo)

      subject
        .moveAggregate(testAggregate.name.get, "parent", fakePrincipal)
        .shouldBe(ServerError("Something went wrong in pgsql"))

      verify(repo).moveAggregate(testAggregate.name.get, "parent")
      verify(producer, never).send(any[ProducerRecord[String, String]])
    }

    "Return the failure from kafka when data can't be sent to kafka" in {
      val producer = mock[KafkaProducer[String, String]]
      val repo = mock[AggregateRepository]

      val expectedRecord =
        new ProducerRecord[String, String](
          AggregateService.TopicName,
          testAggregate.name.get,
          AggregateEvent(MoveAggregate, Some(Aggregate(Some("parent"), None, None))).toJsonString
        )

      when(repo.existsByName(testAggregate.name.get)).thenReturn(true)
      when(repo.existsByName("parent")).thenReturn(true)
      doNothing().when(repo).moveAggregate(testAggregate.name.get, "parent")
      when(producer.send(expectedRecord)).thenThrow(new RuntimeException("Something went wrong in kafka"))
      val fakePrincipal = Principal("name", Set(AuthenticationUtils.ScopeMoveAggregate))

      val subject = new AggregateService(producer, repo)

      subject
        .moveAggregate(testAggregate.name.get, "parent", fakePrincipal)
        .shouldBe(ServerError("Something went wrong in kafka"))

      verify(repo).moveAggregate(testAggregate.name.get, "parent")
      verify(producer).send(expectedRecord)
    }
  }
}
