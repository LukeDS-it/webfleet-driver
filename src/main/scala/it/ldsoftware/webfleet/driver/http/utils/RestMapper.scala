package it.ldsoftware.webfleet.driver.http.utils

trait RestMapper[T, R] {
  def map(t: T): R
}
