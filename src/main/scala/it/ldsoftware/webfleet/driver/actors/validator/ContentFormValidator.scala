package it.ldsoftware.webfleet.driver.actors.validator

import it.ldsoftware.webfleet.driver.actors.model.{ContentForm, WebContent}

class ContentFormValidator {
  def validateNew(contentForm: ContentForm, context: WebContent): List[ValidationError] = {
    List()
  }

  def validateEdit(contentForm: ContentForm): List[ValidationError] = {
    List()
  }
}
