package it.ldsoftware.webfleet.driver.read.model

import java.time.ZonedDateTime

import it.ldsoftware.webfleet.driver.actors.model.WebType

case class ContentRM(
    path: String,
    title: String,
    description: String,
    webType: WebType,
    createdAt: ZonedDateTime,
    parent: Option[String]
)
