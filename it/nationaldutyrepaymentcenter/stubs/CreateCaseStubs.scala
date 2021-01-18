package nationaldutyrepaymentcenter.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import nationaldutyrepaymentcenter.support.WireMockSupport

trait CreateCaseStubs {
  me: WireMockSupport =>

  def givenPegaCreateCaseRequestSucceeds(): Unit =
    stubForPostWithResponse(
      200,
      """{
        |  "ApplicationType" : "NDRC",
        |  "OriginatingSystem" : "Digital",
        |  "Content": {
        |  "ClaimDetails": {
        |      "FormType" : "01",
        |      "CustomRegulationType" : "02",
        |      "ClaimedUnderArticle" : "117",
        |      "Claimant" : "02",
        |      "ClaimType" : "02",
        |      "NoOfEntries" : "10",
        |      "EPU" : "777",
        |      "EntryNumber" : "123456A",
        |      "EntryDate" : "20200101",
        |      "ClaimReason" : "06",
        |      "ClaimDescription" : "this is a claim description",
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
      """{
        |    "Status": "Success",
        |    "StatusText": "Case created successfully",
        |    "CaseID": "PCE201103470D2CC8K0NH3",
        |    "ProcessingDate": "2020-11-03T15:29:28.601Z"
        |}""".stripMargin
    )

  def givenPegaCreateCaseRequestFails(status: Int, errorCode: String, errorMessage: String = ""): Unit =
    stubForPostWithResponse(
      status,
      """{
        |  "ApplicationType" : "NDRC",
        |  "OriginatingSystem" : "Digital",
        |  "Content": {}
        |}""".stripMargin,
      s"""{"errorDetail":{
         |   "timestamp": "2020-11-03T15:29:28.601Z", 
         |   "correlationId": "123123123", 
         |   "errorCode": "$errorCode"
         |   ${if (errorMessage.nonEmpty) s""","errorMessage": "$errorMessage"""" else ""}
         |}}""".stripMargin
    )

  def stubForPostWithResponse(status: Int, payload: String, responseBody: String): Unit =
    stubFor(
      post(urlEqualTo("/cpr/caserequest/ndrc/create/v1"))
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
      post(urlEqualTo("/cpr/caserequest/ndrc/create/v1"))
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
      post(urlEqualTo("/cpr/caserequest/ndrc/create/v1"))
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
