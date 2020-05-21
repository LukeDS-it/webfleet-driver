package it.ldsoftware.webfleet.driver.actors.model

case class ContentChild(path: String, title: String, description: String, webType: WebType) {
  def getParentPath: String = path.substring(0, path.lastIndexOf("/"))
}
