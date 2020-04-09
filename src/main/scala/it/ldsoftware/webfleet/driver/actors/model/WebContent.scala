package it.ldsoftware.webfleet.driver.actors.model

import java.time.ZonedDateTime

case class WebContent(
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
    children: Set[ContentChild]
)
