package it.ldsoftware.webfleet.driver.actors.model

import it.ldsoftware.webfleet.driver.actors.serialization.CborSerializable

case class ContentChild(path: String, title: String, description: String, webType: WebType)
    extends CborSerializable {

  def getParentPath: Option[String] = path.split("/").length match {
    case 1 => None
    case 2 => Some(path.substring(0, path.lastIndexOf("/") + 1))
    case _ => Some(path.substring(0, path.lastIndexOf("/")))
  }

}
