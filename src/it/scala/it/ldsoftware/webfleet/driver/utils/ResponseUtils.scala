package it.ldsoftware.webfleet.driver.utils

import akka.http.scaladsl.model.HttpResponse
import akka.stream.Materializer
import com.typesafe.scalalogging.LazyLogging
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

trait ResponseUtils extends ScalaFutures with LazyLogging {
  def debug(
      future: Future[Future[HttpResponse]]
  )(implicit ec: ExecutionContext, mat: Materializer): Unit = {
    val res = future.flatMap(resp => resp.flatMap(e => e.toStrict(1.second))).futureValue
    logger.debug("DEBUGGING RESPONSE ENTITY:")
    logger.debug(s"Response: $res")
  }
}
