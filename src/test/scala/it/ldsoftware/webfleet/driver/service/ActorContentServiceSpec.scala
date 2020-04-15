package it.ldsoftware.webfleet.driver.service

import java.time.Duration

import akka.cluster.sharding.typed.scaladsl.EntityRef
import akka.util.Timeout
import it.ldsoftware.webfleet.driver.actors.BranchContent.BranchCommand
import it.ldsoftware.webfleet.driver.actors.RootContent._
import it.ldsoftware.webfleet.driver.actors.model._
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

  "The get function" should {
    "return contents of the root content" in {
      val provider = mock[EntityProvider[BranchCommand]]
      val root = mock[EntityRef[RootCommand]]

      val expected = defaultContent
      when(root.ask[RootResponse](any())(any()))
        .thenReturn(Future.successful(RootContentResponse(expected)))

      val subject = new ActorContentService(timeout, root, provider)

      subject.getContent("/").futureValue shouldBe success(expected)
    }

    "return contents of the branch contents" in {}

    "return not found when the content does not exist" in {}
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

}
