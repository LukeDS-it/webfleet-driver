package it.ldsoftware.webfleet.driver.service.impl.util

import akka.cluster.sharding.typed.scaladsl.EntityRef

trait EntityProvider[T] {
  def get(name: String): EntityRef[T]
}
