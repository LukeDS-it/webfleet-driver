package it.ldsoftware.webfleet.driver.services.utils

import it.ldsoftware.webfleet.api.v1.events.AggregateEvent
import it.ldsoftware.webfleet.api.v1.events.AggregateEvent.AddAggregate
import it.ldsoftware.webfleet.api.v1.model.Aggregate
import it.ldsoftware.webfleet.driver.services.utils.EventUtils._
import org.scalatest.{Matchers, WordSpec}

class EventUtilsSpec extends WordSpec with Matchers {

  "The json string maker" should {
    "Produce the correct output for an aggregate event" in {
      new AggregateEvent(AddAggregate, Some(Aggregate(Some("a"), Some("a"), Some("a"))))
        .toJsonString shouldBe """{"eventType":"A","subject":{"name":"a","description":"a","text":"a"}}"""
    }
  }

}
