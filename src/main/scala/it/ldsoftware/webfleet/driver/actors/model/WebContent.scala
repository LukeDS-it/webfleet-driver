package it.ldsoftware.webfleet.driver.actors.model

import java.time.ZonedDateTime

import it.ldsoftware.webfleet.driver.actors.serialization.CborSerializable

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
    children: Map[String, ContentChild]
) extends CborSerializable
