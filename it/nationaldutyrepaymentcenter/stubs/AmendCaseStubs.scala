package nationaldutyrepaymentcenter.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import nationaldutyrepaymentcenter.support.WireMockSupport

trait AmendCaseStubs {
  me: WireMockSupport =>

  def givenPegaAmendCaseRequestSucceeds(): Unit =
    stubForPostWithResponse(
      200,
      """{
        |  "ApplicationType" : "NDRC",
        |  "OriginatingSystem" : "Digital",
        |  "Content": {
        |       "CaseID":"Risk-2507",
        |       "Description":"update request for Risk-2507"
        |    }
        |}""".stripMargin,
      """{
        |    "Status": "Success",
        |    "StatusText": "Case Updated successfully",
        |    "CaseID": "Risk-2507",
        |    "ProcessingDate": "2020-09-24T10:15:43.995Z"
        |}""".stripMargin
    )

  def givenPegaAmendCaseRequestFails(status: Int, errorCode: String, errorMessage: String = ""): Unit =
    stubForPostWithResponse(
      status,
      """{
        |  "ApplicationType" : "NDRC",
        |  "OriginatingSystem" : "Digital",
        |  "Content": {}
        |}""".stripMargin,
      s"""{"errorDetail":{
         |   "timestamp": "2020-09-19T12:12:23.000Z",
         |   "correlationId": "324244343",
         |   "errorCode": "$errorCode"
         |   ${if (errorMessage.nonEmpty) s""","errorMessage": "$errorMessage"""" else ""}
         |}}""".stripMargin
    )

  def stubForPostWithResponse(status: Int, payload: String, responseBody: String): Unit =
    stubFor(
      post(urlEqualTo("/cpr/caserequest/ndrc/update/v1"))
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
      post(urlEqualTo("/cpr/caserequest/ndrc/update/v1"))
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
      post(urlEqualTo("/cpr/caserequest/ndrc/update/v1"))
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

}
