package it.ldsoftware.webfleet.driver.http.utils

import java.security.KeyPairGenerator
import java.security.interfaces.{RSAPrivateKey, RSAPublicKey}

import com.auth0.jwk.{Jwk, JwkProvider}
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import it.ldsoftware.webfleet.driver.security.User
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.{ExecutionContext, Future}

class Auth0UserExtractorSpec extends AnyWordSpec with Matchers with MockitoSugar with ScalaFutures {

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  "The extractor" should {
    "extract principal information with permissions from just JWT" in {
      val kp = keyPair
      val issuer = "issuer"
      val audience = "audience"
      val keyId = "keyId"
      val s = "subject"
      val provider = mock[JwkProvider]
      val jwk = mock[Jwk]
      val pp = mock[PermissionProvider]

      when(provider.get(keyId)).thenReturn(jwk)
      when(jwk.getPublicKey).thenReturn(kp.getPublic)

      val token = JWT
        .create()
        .withKeyId(keyId)
        .withIssuer(issuer)
        .withAudience(audience)
        .withSubject(s)
        .withClaim("scope", "openid edit:aggregate")
        .sign(
          Algorithm.RSA256(
            kp.getPublic.asInstanceOf[RSAPublicKey],
            kp.getPrivate.asInstanceOf[RSAPrivateKey]
          )
        )

      val subject = new Auth0UserExtractor(provider, issuer, audience, pp)

      subject.extractUser(token, None).futureValue shouldBe Some(
        User(s, Set("openid", "edit:aggregate"), Some(token))
      )
    }

    "extract principal information with permissions from JWT and permission provider" in {
      val kp = keyPair
      val issuer = "issuer"
      val audience = "audience"
      val keyId = "keyId"
      val s = "subject"
      val domain = "domain"
      val provider = mock[JwkProvider]
      val jwk = mock[Jwk]
      val pp = mock[PermissionProvider]

      when(provider.get(keyId)).thenReturn(jwk)
      when(jwk.getPublicKey).thenReturn(kp.getPublic)
      when(pp.getPermissions(domain, s))
        .thenReturn(Future.successful(Set("another:permission")))

      val token = JWT
        .create()
        .withKeyId(keyId)
        .withIssuer(issuer)
        .withAudience(audience)
        .withSubject(s)
        .withClaim("scope", "openid edit:aggregate")
        .sign(
          Algorithm.RSA256(
            kp.getPublic.asInstanceOf[RSAPublicKey],
            kp.getPrivate.asInstanceOf[RSAPrivateKey]
          )
        )

      val subject = new Auth0UserExtractor(provider, issuer, audience, pp)

      subject.extractUser(token, Some(domain)).futureValue shouldBe Some(
        User(s, Set("openid", "edit:aggregate", "another:permission"), Some(token))
      )
    }
  }

  private def keyPair = {
    val kpg: KeyPairGenerator = KeyPairGenerator.getInstance("RSA")
    kpg.initialize(2048)
    kpg.genKeyPair
  }
}
