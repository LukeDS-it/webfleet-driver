package it.ldsoftware.webfleet.driver.actors.model

import it.ldsoftware.webfleet.driver.actors.serialization.CborSerializable

case class ContentChild(path: String, title: String, description: String, webType: WebType)
    extends CborSerializable {

  def getParentPath: Option[String] =
    if (path == "/")
      None
    else
      path.split("/").dropRight(1).mkString("/") match {
        case "" => Some("/")
        case s  => Some(s)
      }

}
