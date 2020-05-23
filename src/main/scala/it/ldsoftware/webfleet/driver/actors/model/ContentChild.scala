package it.ldsoftware.webfleet.driver.actors.model

import it.ldsoftware.webfleet.driver.actors.serialization.CborSerializable

case class ContentChild(path: String, title: String, description: String, webType: WebType)
    extends CborSerializable {
  def getParentPath: String = path.substring(0, path.lastIndexOf("/"))
}
