package it.ldsoftware.webfleet.driver.flows.consumers

import akka.Done
import com.typesafe.scalalogging.LazyLogging
import it.ldsoftware.webfleet.commons.flows.EventConsumer
import it.ldsoftware.webfleet.driver.actors.Content
import it.ldsoftware.webfleet.driver.actors.Content.Event
import it.ldsoftware.webfleet.driver.read.model.ContentRM
import it.ldsoftware.webfleet.driver.service.ContentReadService

import scala.concurrent.{ExecutionContext, Future}

// $COVERAGE-OFF$
class ReadSideEventConsumer(readService: ContentReadService)(implicit ec: ExecutionContext)
    extends EventConsumer[Event]
    with LazyLogging {

  override def consume(actorId: String, event: Content.Event): Future[Done] = event match {
    case Content.Created(form, _, time) =>
      val rm = ContentRM(
        form.path,
        form.title,
        form.description,
        form.webType,
        time,
        form.toChild.getParentPath
      )
      logger.debug(s"Adding content $rm")
      readService.insertContent(rm).map(_ => Done)

    case Content.Updated(form, _, _) =>
      logger.debug(s"Updating content $form")
      readService.editContent(actorId, form.title, form.description).map(_ => Done)

    case Content.Deleted(_, _) =>
      logger.debug(s"Deleting content $actorId")
      readService.deleteContent(actorId).map(_ => Done)

    case _ => Future.successful(Done)
  }
}
// $COVERAGE-ON$
