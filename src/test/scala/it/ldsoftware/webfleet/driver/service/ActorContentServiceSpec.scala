package it.ldsoftware.webfleet.driver.service

import java.time.Duration

import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.util.Timeout
import it.ldsoftware.webfleet.driver.actors.Content
import it.ldsoftware.webfleet.driver.actors.Content.MyContent
import it.ldsoftware.webfleet.driver.actors.model._
import it.ldsoftware.webfleet.driver.security.User
import it.ldsoftware.webfleet.driver.service.impl.ActorContentService
import it.ldsoftware.webfleet.driver.service.model._
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.{ExecutionContext, Future}

class ActorContentServiceSpec
    extends AnyWordSpecLike
    with Matchers
    with ScalaFutures
    with IntegrationPatience
    with MockitoSugar {

  implicit val ec: ExecutionContext = ExecutionContext.global

  val timeout: Duration = Duration.ofSeconds(3)
  implicit val askTimeout: Timeout = Timeout.create(timeout)

  "The getContent function" should {
    "return contents of the root content" in {
      val sharding = mock[ClusterSharding]
      val entity = mock[EntityRef[Content.Command]]

      val expected = defaultContent
      when(sharding.entityRefFor(Content.Key, "/")).thenReturn(entity)
      when(entity.ask[Content.Response](any())(any()))
        .thenReturn(Future.successful(MyContent(expected)))

      val subject = new ActorContentService(timeout, sharding)

      subject.getContent("/").futureValue shouldBe success(expected)
    }

    "return contents of the branch contents" in {
      val sharding = mock[ClusterSharding]
      val entity = mock[EntityRef[Content.Command]]

      val expected = defaultContent
      when(sharding.entityRefFor(Content.Key, "/branch")).thenReturn(entity)
      when(entity.ask[Content.Response](any())(any()))
        .thenReturn(Future.successful(MyContent(expected)))

      val subject = new ActorContentService(timeout, sharding)

      subject.getContent("/branch").futureValue shouldBe success(expected)
    }

    "return not found when the content does not exist" in {
      val sharding = mock[ClusterSharding]
      val entity = mock[EntityRef[Content.Command]]

      when(sharding.entityRefFor(Content.Key, "/branch")).thenReturn(entity)
      when(entity.ask[Content.Response](any())(any()))
        .thenReturn(Future.successful(Content.NotFound("/branch")))

      val subject = new ActorContentService(timeout, sharding)

      subject.getContent("/branch").futureValue shouldBe notFound("/branch")
    }

    "return unexpected message when root returns an unexpected message" in {
      val sharding = mock[ClusterSharding]
      val entity = mock[EntityRef[Content.Command]]

      when(sharding.entityRefFor(Content.Key, "/")).thenReturn(entity)
      when(entity.ask[Content.Response](any())(any())).thenReturn(Future.successful(Content.Done))

      val subject = new ActorContentService(timeout, sharding)

      val res = subject.getContent("/").futureValue
      res should be(Symbol("left"))
      res.swap.getOrElse(null) shouldBe an[UnexpectedError]
    }

    "return unexpected message when branch returns an unexpected message" in {
      val sharding = mock[ClusterSharding]
      val entity = mock[EntityRef[Content.Command]]

      when(sharding.entityRefFor(Content.Key, "/branch")).thenReturn(entity)
      when(entity.ask[Content.Response](any())(any())).thenReturn(Future.successful(Content.Done))

      val subject = new ActorContentService(timeout, sharding)

      val res = subject.getContent("/branch").futureValue
      res should be(Symbol("left"))
      res.swap.getOrElse(null) shouldBe an[UnexpectedError]
    }
  }

  "The createContent function" should {
    "return success when the operation was completed" in {
      val sharding = mock[ClusterSharding]
      val entity = mock[EntityRef[Content.Command]]

      when(sharding.entityRefFor(Content.Key, "/path/to/entity")).thenReturn(entity)
      when(entity.ask[Content.Response](any())(any())).thenReturn(Future.successful(Content.Done))

      val subject = new ActorContentService(timeout, sharding)

      val form = defaultForm

      val user = User("name", Set(), None)

      subject.createContent("/path/to/entity", form, user).futureValue shouldBe created(form.path)
    }

    "return invalid form when the form is not valid" in {
      val sharding = mock[ClusterSharding]
      val entity = mock[EntityRef[Content.Command]]

      val errs = List(ValidationError("a", "b", "c"))

      when(sharding.entityRefFor(Content.Key, "/path/to/entity")).thenReturn(entity)
      when(entity.ask[Content.Response](any())(any()))
        .thenReturn(Future.successful(Content.Invalid(errs)))

      val subject = new ActorContentService(timeout, sharding)

      val form = defaultForm

      val user = User("name", Set(), None)

      subject.createContent("/path/to/entity", form, user).futureValue shouldBe invalid(errs)
    }

    "return an unexpected failure if there was something wrong" in {
      val sharding = mock[ClusterSharding]
      val entity = mock[EntityRef[Content.Command]]

      val err = new Exception("Error while creating content")

      when(sharding.entityRefFor(Content.Key, "/")).thenReturn(entity)
      when(entity.ask[Content.Response](any())(any()))
        .thenReturn(Future.successful(Content.UnexpectedError(err)))

      val subject = new ActorContentService(timeout, sharding)

      val form = defaultForm

      val user = User("name", Set(), None)

      subject.createContent("/", form, user).futureValue shouldBe unexpectedError(
        err,
        "Error while creating content"
      )
    }

    "return unexpected message when the root returns an unexpected message" in {
      val sharding = mock[ClusterSharding]
      val entity = mock[EntityRef[Content.Command]]

      when(sharding.entityRefFor(Content.Key, "/")).thenReturn(entity)
      when(entity.ask[Content.Response](any())(any()))
        .thenReturn(Future.successful(Content.MyContent(null)))

      val subject = new ActorContentService(timeout, sharding)

      val form = defaultForm

      val user = User("name", Set(), None)

      val res = subject.createContent("/", form, user).futureValue
      res should be(Symbol("left"))
      res.swap.getOrElse(null) shouldBe an[UnexpectedError]
    }
  }

  "The editContent function" should {
    "return success when the operation was completed" in {
      val sharding = mock[ClusterSharding]
      val entity = mock[EntityRef[Content.Command]]

      when(sharding.entityRefFor(Content.Key, "/")).thenReturn(entity)
      when(entity.ask[Content.Response](any())(any()))
        .thenReturn(Future.successful(Content.Done))

      val subject = new ActorContentService(timeout, sharding)

      val form = editForm

      val user = User("name", Set(), None)

      subject.editContent("/", form, user).futureValue shouldBe noOutput
    }

    "return invalid form when the form is not valid" in {
      val sharding = mock[ClusterSharding]
      val entity = mock[EntityRef[Content.Command]]

      val errs = List(ValidationError("a", "b", "c"))

      when(sharding.entityRefFor(Content.Key, "/")).thenReturn(entity)
      when(entity.ask[Content.Response](any())(any()))
        .thenReturn(Future.successful(Content.Invalid(errs)))

      val subject = new ActorContentService(timeout, sharding)

      val form = editForm

      val user = User("name", Set(), None)

      subject.editContent("/", form, user).futureValue shouldBe invalid(errs)
    }

    "return an unexpected failure if there was something wrong" in {
      val sharding = mock[ClusterSharding]
      val entity = mock[EntityRef[Content.Command]]

      val err = new Exception("Error while updating root")

      when(sharding.entityRefFor(Content.Key, "/")).thenReturn(entity)
      when(entity.ask[Content.Response](any())(any()))
        .thenReturn(Future.successful(Content.UnexpectedError(err)))

      val subject = new ActorContentService(timeout, sharding)

      val form = editForm

      val user = User("name", Set(), None)

      subject.editContent("/", form, user).futureValue shouldBe unexpectedError(
        err,
        "Error while updating root"
      )
    }

    "return unexpected message when the root returns an unexpected message" in {
      val sharding = mock[ClusterSharding]
      val entity = mock[EntityRef[Content.Command]]

      when(sharding.entityRefFor(Content.Key, "/")).thenReturn(entity)
      when(entity.ask[Content.Response](any())(any()))
        .thenReturn(Future.successful(Content.MyContent(null)))

      val subject = new ActorContentService(timeout, sharding)

      val form = editForm

      val user = User("name", Set(), None)

      val res = subject.editContent("/", form, user).futureValue
      res should be(Symbol("left"))
      res.swap.getOrElse(null) shouldBe an[UnexpectedError]
    }

  }

  "The deleteContent function" should {
    "return no output when operation was completed" in {
      val sharding = mock[ClusterSharding]
      val entity = mock[EntityRef[Content.Command]]

      when(sharding.entityRefFor(Content.Key, "/child")).thenReturn(entity)
      when(entity.ask[Content.Response](any())(any()))
        .thenReturn(Future.successful(Content.Done))

      val subject = new ActorContentService(timeout, sharding)

      val user = User("name", Set(), None)

      subject.deleteContent("/child", user).futureValue shouldBe noOutput
    }

    "return not found when trying to delete a non existing branch" in {
      val sharding = mock[ClusterSharding]
      val entity = mock[EntityRef[Content.Command]]

      when(sharding.entityRefFor(Content.Key, "/child")).thenReturn(entity)
      when(entity.ask[Content.Response](any())(any()))
        .thenReturn(Future.successful(Content.NotFound("/child")))

      val subject = new ActorContentService(timeout, sharding)

      val user = User("name", Set(), None)

      subject.deleteContent("/child", user).futureValue shouldBe notFound("/child")
    }

    "return insufficient permissions if the user doesn't have enough permissions" in {
      val sharding = mock[ClusterSharding]
      val entity = mock[EntityRef[Content.Command]]

      when(sharding.entityRefFor(Content.Key, "/child")).thenReturn(entity)
      when(entity.ask[Content.Response](any())(any()))
        .thenReturn(Future.successful(Content.UnAuthorized))

      val subject = new ActorContentService(timeout, sharding)

      val user = User("name", Set(), None)

      subject.deleteContent("/child", user).futureValue shouldBe forbidden
    }

    "return unexpected message in all other cases" in {
      val sharding = mock[ClusterSharding]
      val entity = mock[EntityRef[Content.Command]]

      when(sharding.entityRefFor(Content.Key, "/child")).thenReturn(entity)
      when(entity.ask[Content.Response](any())(any()))
        .thenReturn(Future.successful(Content.MyContent(null)))

      val subject = new ActorContentService(timeout, sharding)

      val user = User("name", Set(), None)

      val res = subject.deleteContent("/child", user).futureValue
      res should be(Symbol("left"))
      res.swap.getOrElse(null) shouldBe an[UnexpectedError]
    }
  }

  def defaultContent: WebContent = WebContent(
    "parent",
    "/parent",
    Folder,
    "desc",
    "txt",
    "t",
    "i",
    None,
    Published,
    "author",
    None,
    None,
    Map()
  )

  def defaultForm: CreateForm = CreateForm(
    "title",
    "path",
    Folder,
    "description",
    "text",
    "theme",
    "icon",
    None,
    None
  )

  def editForm: UpdateForm = UpdateForm(
    Some("title"),
    Some("descr"),
    Some("text"),
    Some("theme"),
    Some("icon"),
    None,
    Some(Published)
  )

}
