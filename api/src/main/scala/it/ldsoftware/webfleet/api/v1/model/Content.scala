package it.ldsoftware.webfleet.api.v1.model

case class Content(title: Option[String],
                   description: Option[String],
                   text: Option[String],
                   permalink: Option[String],
                   author: Option[String],
                   status: Option[ContentStatus])
