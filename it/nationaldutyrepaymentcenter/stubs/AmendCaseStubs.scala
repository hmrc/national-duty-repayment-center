package nationaldutyrepaymentcenter.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.Scenario
import nationaldutyrepaymentcenter.support.WireMockSupport
import org.scalatest.concurrent.Eventually.eventually
import play.mvc.Http.MimeTypes

trait AmendCaseStubs {
  me: WireMockSupport =>

  private val UPDATE_CASE_URL = "/cpr/caserequest/ndrc/update/v1"

  def givenEISTimeout(): Unit =
    stubFor(
      post(
        urlEqualTo(UPDATE_CASE_URL)
      ).willReturn(
        aResponse()
          .withStatus(499)
      )
    )

  def givenPegaAmendCaseRequestSucceeds(correlationId: String, caseRef: String = "Risk-2507"): Unit =
    stubForPostWithResponse(200, s"""{
        |"AcknowledgementReference" : "${correlationId.replace("-", "")}",
        |  "ApplicationType" : "NDRC",
        |  "OriginatingSystem" : "Digital",
        |  "Content": {
        |       "CaseID":"$caseRef",
        |       "Description":"update request for Risk-2507: Value £199.99"
        |    }
        |}""".stripMargin, s"""{
        |    "Status": "Success",
        |    "StatusText": "Case Updated successfully",
        |    "CaseID": "$caseRef",
        |    "ProcessingDate": "2020-09-24T10:15:43.995Z"
        |}""".stripMargin)

  def givenPegaAmendCaseRequestFails(
    status: Int,
    errorCode: String,
    errorMessage: String = "",
    correlationId: String = "324244343"
  ): Unit =
    stubForPostWithResponse(status, """{
        |  "ApplicationType" : "NDRC",
        |  "OriginatingSystem" : "Digital",
        |  "Content": {}
        |}""".stripMargin, s"""{"errorDetail":{
         |   "timestamp": "2020-09-19T12:12:23.000Z",
         |   "correlationId": "$correlationId",
         |   "errorCode": "$errorCode"
         |   ${if (errorMessage.nonEmpty) s""","errorMessage": "$errorMessage"""" else ""}
         |}}""".stripMargin)

  def givenPegaAmendCaseRequestSucceedsAfterTwoRetryResponses(caseRef: String): Unit = {

    stubFor(
      post(urlEqualTo(UPDATE_CASE_URL))
        .inScenario("retry")
        .whenScenarioStateIs(Scenario.STARTED)
        .willSetStateTo("stllno")
        .willReturn(aResponse().withStatus(429).withHeader("Retry-After", "300"))
    )

    stubFor(
      post(urlEqualTo(UPDATE_CASE_URL))
        .inScenario("retry")
        .whenScenarioStateIs("stllno")
        .willSetStateTo("oknow")
        .willReturn(aResponse().withStatus(429).withHeader("Retry-After", "300"))
    )

    stubFor(
      post(urlEqualTo(UPDATE_CASE_URL))
        .inScenario("retry")
        .whenScenarioStateIs("oknow")
        .willSetStateTo(Scenario.STARTED)
        .willReturn(
          aResponse().withStatus(200).withBody(s"""{
                                                            |    "Status": "Success",
                                                            |    "StatusText": "Case Updated successfully",
                                                            |    "CaseID": "$caseRef",
                                                            |    "ProcessingDate": "2020-11-03T15:29:28.601Z"
                                                            |}""".stripMargin).withHeader(
            "Content-Type",
            MimeTypes.JSON
          )
        )
    )
  }

  def stubForPostWithResponse(status: Int, payload: String, responseBody: String): Unit =
    stubFor(
      post(urlEqualTo(UPDATE_CASE_URL))
        .withHeader("x-correlation-id", matching("[A-Za-z0-9-]{36}"))
        .withHeader("CustomProcessesHost", equalTo("Digital"))
        .withHeader("date", matching("[A-Za-z0-9,: ]{29}"))
        .withHeader("accept", equalTo("application/json"))
        .withHeader("content-Type", equalTo("application/json"))
        .withHeader("authorization", equalTo("Bearer dummy-it-token"))
        .withHeader("environment", equalTo("it"))
        .withRequestBody(equalToJson(payload, true, true))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withHeader("Content-Type", "application/json")
            .withBody(responseBody)
        )
    )

  def givenPegaAmendCaseRequestRespondsWithHtml(): Unit =
    stubFor(
      post(urlEqualTo(UPDATE_CASE_URL))
        .withHeader("x-correlation-id", matching("[A-Za-z0-9-]{36}"))
        .withHeader("CustomProcessesHost", equalTo("Digital"))
        .withHeader("date", matching("[A-Za-z0-9,: ]{29}"))
        .withHeader("accept", equalTo("application/json"))
        .withHeader("content-Type", equalTo("application/json"))
        .withHeader("authorization", equalTo("Bearer dummy-it-token"))
        .withHeader("environment", equalTo("it"))
        .willReturn(
          aResponse()
            .withStatus(400)
            .withHeader("Content-Type", "text/html")
            .withBody(
              """<html>\r\n<head><title>400 Bad Request</title></head>\r\n<body bgcolor=\"white\">\r\n<center><h1>400 Bad Request</h1></center>\r\n<hr><center>nginx</center>\r\n</body>\r\n</html>\r\n\"""
            )
        )
    )

  def givenPegaAmendCaseRequestRespondsWith403WithoutContent(): Unit =
    stubFor(
      post(urlEqualTo(UPDATE_CASE_URL))
        .withHeader("x-correlation-id", matching("[A-Za-z0-9-]{36}"))
        .withHeader("CustomProcessesHost", equalTo("Digital"))
        .withHeader("date", matching("[A-Za-z0-9,: ]{29}"))
        .withHeader("accept", equalTo("application/json"))
        .withHeader("content-Type", equalTo("application/json"))
        .withHeader("authorization", equalTo("Bearer dummy-it-token"))
        .withHeader("environment", equalTo("it"))
        .willReturn(
          aResponse()
            .withStatus(403)
        )
    )

  def verifyAmendCaseSent(
    caseId: String = "Risk-2507",
    description: String = "update request for Risk-2507: Value £199.99"
  ) = {

    val json = s"""{
                  |  "Content": {
                  |       "CaseID":"$caseId",
                  |       "Description":"$description"
                  |    }
                  |}""".stripMargin

    eventually(
      verify(
        1,
        postRequestedFor(urlPathMatching(UPDATE_CASE_URL))
          .withRequestBody(equalToJson(json.stripMargin, true, true))
      )
    )
  }

}
