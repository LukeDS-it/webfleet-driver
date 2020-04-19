package it.ldsoftware.webfleet.driver.service

import java.time.Duration

import akka.cluster.sharding.typed.scaladsl.EntityRef
import akka.util.Timeout
import it.ldsoftware.webfleet.driver.actors.BranchContent.{apply => _, _}
import it.ldsoftware.webfleet.driver.actors.RootContent._
import it.ldsoftware.webfleet.driver.actors.model._
import it.ldsoftware.webfleet.driver.security.User
import it.ldsoftware.webfleet.driver.service.impl.ActorContentService
import it.ldsoftware.webfleet.driver.service.impl.util.EntityProvider
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
      val provider = mock[EntityProvider[BranchCommand]]
      val root = mock[EntityRef[RootCommand]]

      val expected = defaultContent
      when(root.ask[RootResponse](any())(any()))
        .thenReturn(Future.successful(RootContentResponse(expected)))

      val subject = new ActorContentService(timeout, root, provider)

      subject.getContent("/").futureValue shouldBe success(expected)
    }

    "return contents of the branch contents" in {
      val provider = mock[EntityProvider[BranchCommand]]
      val root = mock[EntityRef[RootCommand]]
      val branch = mock[EntityRef[BranchCommand]]
      val expected = defaultContent

      when(provider.get("/branch")).thenReturn(branch)
      when(branch.ask[BranchResponse](any())(any()))
        .thenReturn(Future.successful(BranchContentResponse(expected)))

      val subject = new ActorContentService(timeout, root, provider)

      subject.getContent("/branch").futureValue shouldBe success(expected)
    }

    "return not found when the content does not exist" in {
      val provider = mock[EntityProvider[BranchCommand]]
      val root = mock[EntityRef[RootCommand]]
      val branch = mock[EntityRef[BranchCommand]]

      when(provider.get("/branch")).thenReturn(branch)
      when(branch.ask[BranchResponse](any())(any())).thenReturn(Future.successful(BranchNotFound))

      val subject = new ActorContentService(timeout, root, provider)

      subject.getContent("/branch").futureValue shouldBe notFound("/branch")
    }

    "return unexpected message when root returns an unexpected message" in {
      val provider = mock[EntityProvider[BranchCommand]]
      val root = mock[EntityRef[RootCommand]]

      when(root.ask[RootResponse](any())(any())).thenReturn(Future.successful(RootDone))

      val subject = new ActorContentService(timeout, root, provider)

      val res = subject.getContent("/").futureValue
      res should be(Symbol("left"))
      res.swap.getOrElse(null) shouldBe an[UnexpectedError]
    }

    "return unexpected message when branch returns an unexpected message" in {
      val provider = mock[EntityProvider[BranchCommand]]
      val root = mock[EntityRef[RootCommand]]
      val branch = mock[EntityRef[BranchCommand]]

      when(provider.get("/branch")).thenReturn(branch)
      when(branch.ask[BranchResponse](any())(any())).thenReturn(Future.successful(BranchDone))

      val subject = new ActorContentService(timeout, root, provider)

      val res = subject.getContent("/branch").futureValue
      res should be(Symbol("left"))
      res.swap.getOrElse(null) shouldBe an[UnexpectedError]
    }
  }

  "The createContent function on root" should {
    "return success when the operation was completed" in {
      val provider = mock[EntityProvider[BranchCommand]]
      val root = mock[EntityRef[RootCommand]]

      when(root.ask[RootResponse](any())(any())).thenReturn(Future.successful(RootDone))

      val subject = new ActorContentService(timeout, root, provider)

      val form = defaultForm

      val user = User("name", Set(), None)

      subject.createContent("/", form, user).futureValue shouldBe success(form.path)
    }

    "return invalid form when the form is not valid" in {
      val provider = mock[EntityProvider[BranchCommand]]
      val root = mock[EntityRef[RootCommand]]

      val errs = List(ValidationError("a", "b", "c"))

      when(root.ask[RootResponse](any())(any())).thenReturn(Future.successful(InvalidForm(errs)))

      val subject = new ActorContentService(timeout, root, provider)

      val form = defaultForm

      val user = User("name", Set(), None)

      subject.createContent("/", form, user).futureValue shouldBe invalid(errs)
    }

    "return an unexpected failure if there was something wrong" in {
      val provider = mock[EntityProvider[BranchCommand]]
      val root = mock[EntityRef[RootCommand]]

      val err = new Exception("Error")

      when(root.ask[RootResponse](any())(any()))
        .thenReturn(Future.successful(UnexpectedRootFailure(err)))

      val subject = new ActorContentService(timeout, root, provider)

      val form = defaultForm

      val user = User("name", Set(), None)

      subject.createContent("/", form, user).futureValue shouldBe unexpectedError(
        err,
        "Error while creating content"
      )
    }

    "return forbidden if there are permission problems" in {
      val provider = mock[EntityProvider[BranchCommand]]
      val root = mock[EntityRef[RootCommand]]

      when(root.ask[RootResponse](any())(any()))
        .thenReturn(Future.successful(InsufficientRootPermissions))

      val subject = new ActorContentService(timeout, root, provider)

      val form = defaultForm

      val user = User("name", Set(), None)

      subject.createContent("/", form, user).futureValue shouldBe forbidden
    }

    "return unexpected message when the root returns an unexpected message" in {
      val provider = mock[EntityProvider[BranchCommand]]
      val root = mock[EntityRef[RootCommand]]

      when(root.ask[RootResponse](any())(any()))
        .thenReturn(Future.successful(RootContentResponse(null)))

      val subject = new ActorContentService(timeout, root, provider)

      val form = defaultForm

      val user = User("name", Set(), None)

      val res = subject.createContent("/", form, user).futureValue
      res should be(Symbol("left"))
      res.swap.getOrElse(null) shouldBe an[UnexpectedError]
    }
  }

  "The createContent function on branch" should {
    "return success when the operation was completed" in {
      val provider = mock[EntityProvider[BranchCommand]]
      val root = mock[EntityRef[RootCommand]]
      val branch = mock[EntityRef[BranchCommand]]

      when(provider.get("/child")).thenReturn(branch)
      when(branch.ask[BranchResponse](any())(any())).thenReturn(Future.successful(BranchDone))

      val subject = new ActorContentService(timeout, root, provider)

      val form = defaultForm

      val user = User("name", Set(), None)

      subject.createContent("/child", form, user).futureValue shouldBe success(form.path)
    }
    "return invalid form when the form is not valid" in {}
    "return an unexpected failure if there was something wrong" in {}
    "return forbidden if there are permission problems" in {}
    "return unexpected message when the branch returns an unexpected message" in {}
  }

  "The editContent function on root" should {}

  "The editContent function on branch" should {}

  "The deleteContent function" should {}

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

  def defaultForm: CreationForm = CreationForm(
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

}