package nationaldutyrepaymentcenter.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.http.Fault
import com.github.tomakehurst.wiremock.stubbing.Scenario
import nationaldutyrepaymentcenter.support.WireMockSupport
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.mvc.Http.MimeTypes

trait CreateCaseStubs {
  me: WireMockSupport =>

  private val CREATE_CASE_URL = "/cpr/caserequest/ndrc/create/v1"

  def givenEISTimeout(): Unit =
    stubFor(
      post(
        urlEqualTo(CREATE_CASE_URL)
      ).willReturn(
        aResponse()
          .withFixedDelay(25000)
      )
    )

  def givenEISCallFailsUnexpectedly(): Unit =
    stubFor(
      post(
        urlEqualTo(CREATE_CASE_URL)
      ).willReturn(
        aResponse()
          .withStatus(INTERNAL_SERVER_ERROR)
          .withFault(Fault.MALFORMED_RESPONSE_CHUNK)
      )
    )

  def givenPegaCreateCaseRequestSucceeds(caseRef: String = "PCE201103470D2CC8K0NH3"): Unit =
    stubForPostWithResponse(
      200,
      """{
        |  "ApplicationType" : "NDRC",
        |  "OriginatingSystem" : "Digital",
        |  "Content": {
        |  "ClaimDetails": {
        |      "FormType" : "01",
        |      "CustomRegulationType" : "02",
        |      "ClaimedUnderArticle" : "051",
        |      "Claimant" : "02",
        |      "ClaimType" : "02",
        |      "NoOfEntries" : "10",
        |      "EPU" : "777",
        |      "EntryNumber" : "123456A",
        |      "EntryDate" : "20200101",
        |      "ClaimReason" : "06",
        |      "ClaimDescription" : "this is a claim description for £123",
        |      "DateReceived" : "20200805",
        |      "ClaimDate" : "20200805",
        |      "PayeeIndicator" : "01",
        |      "PaymentMethod" : "02"
        |    },
        |    "AgentDetails" : {
        |      "IsVATRegistered" : "true",
        |      "EORI" : "GB123456789123456",
        |      "Name" : "Joe Bloggs",
        |      "Address" : {
        |        "AddressLine1" : "line 1",
        |        "AddressLine2" : "line 2",
        |        "City" : "city",
        |        "Region" : "region",
        |        "CountryCode" : "GB",
        |        "PostalCode" : "ZZ111ZZ",
        |        "TelephoneNumber" : "12345678",
        |        "EmailAddress" : "example@example.com"
        |      }
        |    },
        |    "ImporterDetails" : {
        |      "IsVATRegistered" : "true",
        |      "EORI" : "GB123456789123456",
        |      "Name" : "Joe Bloggs",
        |      "Address" : {
        |        "AddressLine1" : "line 1",
        |        "AddressLine2" : "line 2",
        |        "City" : "city",
        |        "Region" : "region",
        |        "CountryCode" : "GB",
        |        "PostalCode" : "ZZ111ZZ",
        |        "TelephoneNumber" : "12345678",
        |        "EmailAddress" : "example@example.com"
        |      }
        |    },
        |    "BankDetails" : {
        |      "ImporterBankDetails" : {
        |        "AccountName" : "account name",
        |        "SortCode" : "123456",
        |        "AccountNumber" : "12345678"
        |      },
        |      "AgentBankDetails" : {
        |        "AccountName" : "account name",
        |        "SortCode" : "123456",
        |        "AccountNumber" : "12345678"
        |      }
        |    },
        |    "DutyTypeTaxDetails" : {
        |      "DutyTypeTaxList" : [
        |        {
        |          "Type" : "01",
        |          "PaidAmount" : "100.00",
        |          "DueAmount" : "50.00",
        |          "ClaimAmount" : "50.00"
        |        },
        |        {
        |          "Type" : "02",
        |          "PaidAmount" : "100.00",
        |          "DueAmount" : "50.00",
        |          "ClaimAmount" : "50.00"
        |        },
        |        {
        |          "Type" : "03",
        |          "PaidAmount" : "100.00",
        |          "DueAmount" : "50.00",
        |          "ClaimAmount" : "50.00"
        |        }
        |      ]
        |    },
        |    "DocumentList" : [
        |      {
        |        "Type" : "03",
        |        "Description" : "this is a copy of c88"
        |      },
        |      {
        |        "Type" : "01",
        |        "Description" : "this is an invoice"
        |      },
        |      {
        |        "Type" : "04",
        |        "Description" : "this is a packing list"
        |      }
        |    ]
        |    }
        |}""".stripMargin,
      s"""{
        |    "Status": "Success",
        |    "StatusText": "Case created successfully",
        |    "CaseID": "$caseRef",
        |    "ProcessingDate": "2020-11-03T15:29:28.601Z"
        |}""".stripMargin
    )

  def givenPegaCreateCaseRequestFails(status: Int, errorCode: String, errorMessage: String = "", correlationId: String = "123123123"): Unit =
    stubForPostWithResponse(
      status = status,
      payload =
        """{
          |  "ApplicationType" : "NDRC",
          |  "OriginatingSystem" : "Digital",
          |  "Content": {}
          |}""".stripMargin,
      responseBody =
        s"""{"errorDetail":{
           |   "timestamp": "2020-11-03T15:29:28.601Z",
           |   "correlationId": "$correlationId",
           |   "errorCode": "$errorCode"
           |   ${if (errorMessage.nonEmpty) s""","errorMessage": "$errorMessage"""" else ""}
           |}}""".stripMargin
    )

  def givenPegaCreateCaseRequestSucceedsAfterTwoRetryResponses(caseRef: String): Unit = {

    stubFor(
      post(urlEqualTo(CREATE_CASE_URL))
        .inScenario("retry")
        .whenScenarioStateIs(Scenario.STARTED)
        .willSetStateTo("stllno")
        .willReturn(aResponse().withStatus(429).withHeader("Retry-After", "300"))
    )

    stubFor(
      post(urlEqualTo(CREATE_CASE_URL))
        .inScenario("retry")
        .whenScenarioStateIs("stllno")
        .willSetStateTo("oknow")
        .willReturn(aResponse().withStatus(429).withHeader("Retry-After", "300"))
    )

    stubFor(
      post(urlEqualTo(CREATE_CASE_URL))
        .inScenario("retry")
        .whenScenarioStateIs("oknow")
        .willSetStateTo(Scenario.STARTED)
        .willReturn(aResponse().withStatus(200).withBody(s"""{
                                                            |    "Status": "Success",
                                                            |    "StatusText": "Case created successfully",
                                                            |    "CaseID": "$caseRef",
                                                            |    "ProcessingDate": "2020-11-03T15:29:28.601Z"
                                                            |}""".stripMargin).withHeader("Content-Type", MimeTypes.JSON))
    )
  }

  def stubForPostWithResponse(status: Int, payload: String, responseBody: String): Unit =
    stubFor(
      post(urlEqualTo(CREATE_CASE_URL))
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

  def givenPegaCreateCaseRequestRespondsWithHtml(): Unit =
    stubFor(
      post(urlEqualTo(CREATE_CASE_URL))
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

  def givenPegaCreateCaseRequestRespondsWith403WithoutContent(): Unit =
    stubFor(
      post(urlEqualTo(CREATE_CASE_URL))
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
