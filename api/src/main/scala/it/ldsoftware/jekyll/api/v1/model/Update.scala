package it.ldsoftware.jekyll.api.v1.model

import java.time.LocalDateTime

case class Update(title: Option[String],
                  description: Option[String],
                  text: Option[String],
                  permalink: Option[String],
                  author: Option[String],
                  contentStatus: Option[ContentStatus],
                  publishDate: Option[LocalDateTime])
