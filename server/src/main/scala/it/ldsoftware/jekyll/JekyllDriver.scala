package it.ldsoftware.jekyll

import com.twitter.finagle.{Http, Service, http}
import com.twitter.util.{Await, Future}

import scala.util.Properties

object JekyllDriver extends App {
  val service = new Service[http.Request, http.Response] {
    def apply(req: http.Request): Future[http.Response] =
      Future.value(
        http.Response(req.version, http.Status.Ok)
      )
  }

  val port = Properties.envOrElse("PORT", "8080")
  println(s"binding on $port")
  val server = Http.serve(s":$port", service)
  Await.ready(server)
}
