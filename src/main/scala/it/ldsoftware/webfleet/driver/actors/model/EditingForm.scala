package it.ldsoftware.webfleet.driver.actors.model

case class EditingForm(
    title: String,
    description: String,
    text: String,
    theme: String,
    icon: String,
    event: Option[WebCalendar],
    status: ContentStatus
)
