package it.ldsoftware.webfleet.driver.actors.model

case class ContentForm(
    title: String,
    path: String,
    webType: WebType,
    description: String,
    text: String,
    theme: String,
    icon: String,
    event: Option[WebCalendar]
)
