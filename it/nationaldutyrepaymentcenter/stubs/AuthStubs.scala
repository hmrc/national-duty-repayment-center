package nationaldutyrepaymentcenter.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import nationaldutyrepaymentcenter.support.WireMockSupport


trait AuthStubs {
  me: WireMockSupport =>

  def givenAuthorised() =
    stubForAuthAuthorise(
      s"""
         |{
         |  "authorise": [
         |    { "authProviders": ["GovernmentGateway"] }
         |  ]
         |}
           """.stripMargin,
      "{}"
    )

  def givenUnauthorisedWith(mdtpDetail: String): Unit =
    stubFor(
      post(urlEqualTo("/auth/authorise"))
        .willReturn(
          aResponse()
            .withStatus(401)
            .withHeader("WWW-Authenticate", s"""MDTP detail="$mdtpDetail"""")
        )
    )

  def stubForAuthAuthorise(payload: String, responseBody: String): Unit = {
    stubFor(
      post(urlEqualTo("/auth/authorise"))
        .atPriority(1)
        .withRequestBody(equalToJson(payload, true, true))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(responseBody)
        )
    )

    stubFor(
      post(urlEqualTo("/auth/authorise"))
        .atPriority(2)
        .willReturn(
          aResponse()
            .withStatus(401)
            .withHeader("WWW-Authenticate", "MDTP detail=\"InsufficientEnrolments\"")
        )
    )
  }

  def verifyAuthoriseAttempt(): Unit =
    verify(1, postRequestedFor(urlEqualTo("/auth/authorise")))

}
