package it.ldsoftware.webfleet.driver.actors.model

sealed trait WebType

case object Page extends WebType

case object Folder extends WebType

case object Calendar extends WebType
