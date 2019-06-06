package it.ldsoftware.webfleet.driver.routes.utils

import java.security.KeyPairGenerator
import java.security.interfaces.{RSAPrivateKey, RSAPublicKey}

import com.auth0.jwk.{Jwk, JwkProvider}
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import it.ldsoftware.webfleet.api.v1.auth.Principal
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Matchers, WordSpec}

class Auth0PrincipalExtractorSpec extends WordSpec with Matchers with MockitoSugar {

  "The extractor" should {
    "correctly extract principal information" in {
      val kp = keyPair
      val issuer = "issuer"
      val audience = "audience"
      val keyId = "keyId"
      val s = "subject"
      val provider = mock[JwkProvider]
      val jwk = mock[Jwk]

      when(provider.get(keyId)).thenReturn(jwk)
      when(jwk.getPublicKey).thenReturn(kp.getPublic)

      val token = JWT.create()
        .withKeyId(keyId)
        .withIssuer(issuer)
        .withAudience(audience)
        .withSubject(s)
        .withClaim("scope", "openid edit:aggregate")
        .sign(Algorithm.RSA256(kp.getPublic.asInstanceOf[RSAPublicKey], kp.getPrivate.asInstanceOf[RSAPrivateKey]))

      val subject = new Auth0PrincipalExtractor(provider, issuer, audience)

      subject.extractPrincipal(token) shouldBe Some(Principal(s, Set("openid", "edit:aggregate"), Some(token)))
    }
  }

  private def keyPair = {
    val kpg: KeyPairGenerator = KeyPairGenerator.getInstance("RSA")
    kpg.initialize(2048)
    kpg.genKeyPair
  }
}
