package it.ldsoftware.webfleet.driver.utils

import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}

trait AuthUtils {

  def jwtHeader(name: String, permissions: Set[String]): HttpHeader =
    Authorization(OAuth2BearerToken(generateToken(name, permissions)))

  def generateToken(name: String, perms: Set[String]): String = ""
}
