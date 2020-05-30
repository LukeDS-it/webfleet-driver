package it.ldsoftware.webfleet.driver.flows.consumers

import it.ldsoftware.webfleet.driver.actors.Content
import it.ldsoftware.webfleet.driver.flows.ContentEventConsumer
import it.ldsoftware.webfleet.driver.read.model.ContentRM
import it.ldsoftware.webfleet.driver.service.ContentReadService

// $COVERAGE-OFF$
class ReadSideEventConsumer(readService: ContentReadService) extends ContentEventConsumer {

  override def consume(actorId: String, event: Content.Event): Unit = event match {
    case Content.Created(form, _, time) =>
      val rm = ContentRM(
        form.path,
        form.title,
        form.description,
        form.webType,
        time,
        form.toChild.getParentPath
      )
      readService.insertContent(rm)

    case Content.Updated(form, _, _) =>
      readService.editContent(actorId, form.title, form.description)

    case Content.Deleted(_, _) =>
      readService.deleteContent(actorId)

    case _ => ()
  }
}
// $COVERAGE-ON$
