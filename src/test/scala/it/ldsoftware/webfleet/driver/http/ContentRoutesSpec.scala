package it.ldsoftware.webfleet.driver.http

import it.ldsoftware.webfleet.driver.http.utils.BaseHttpSpec

class ContentRoutesSpec extends BaseHttpSpec {

  "The GET path" should {
    "return the information of a page" in {}

    "return the information of a directory" in {}

    "return the information of an event" in {}
  }

  "The POST path" should {
    "return a Created response when anything has been created" in {}

    "return with a 400 if the form data is invalid" in {}
  }

  "The PUT path" should {
    "return with a No Content response when anything has been updated" in {}

    "return with a 400 if the form data is invalid" in {}
  }

  "The DELETE path" should {
    "return with a No Content response when anything has been deleted" in {}
  }

}
