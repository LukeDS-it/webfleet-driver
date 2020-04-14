package it.ldsoftware.webfleet.driver.http.model.out

import java.time.ZonedDateTime

import it.ldsoftware.webfleet.driver.actors.model._

case class WebContentDto(
    title: String,
    path: String,
    webType: WebType,
    description: String,
    text: String,
    theme: String,
    icon: String,
    event: Option[WebCalendar],
    status: ContentStatus,
    author: String,
    created: Option[ZonedDateTime],
    published: Option[ZonedDateTime],
    children: Map[String, ContentChild],
    _links: Map[String, Link]
)

object WebContentDto {
  def apply(serviceValue: WebContent): WebContentDto = {
    val links = Map(
      "self" -> Link(s"${serviceValue.path}", "", "GET")
    )

    new WebContentDto(
      serviceValue.title,
      serviceValue.path,
      serviceValue.webType,
      serviceValue.description,
      serviceValue.text,
      serviceValue.theme,
      serviceValue.icon,
      serviceValue.event,
      serviceValue.status,
      serviceValue.author,
      serviceValue.created,
      serviceValue.published,
      serviceValue.children,
      links
    )
  }
}
