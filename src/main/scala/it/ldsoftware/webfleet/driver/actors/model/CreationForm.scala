package it.ldsoftware.webfleet.driver.actors.model

case class CreationForm(
    title: String,
    path: String,
    webType: WebType,
    description: String,
    text: String,
    theme: String,
    icon: String,
    contentStatus: Option[ContentStatus],
    event: Option[WebCalendar]
) {
  def toChild: ContentChild = ContentChild(path, title, description, webType)
}
