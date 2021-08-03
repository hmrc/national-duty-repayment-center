package nationaldutyrepaymentcenter.stubs

import nationaldutyrepaymentcenter.support.WireMockSupport
import play.api.libs.json.Json
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.{MultiFileTransferRequest, MultiFileTransferResult}

trait FileTransferStubs {
  me: WireMockSupport =>

  def givenFileTransmissionsMultipleSucceeds(
    multiFileTransferRequest: MultiFileTransferRequest = multiFileRequest(),
    multiFileTransferResult: MultiFileTransferResult = multiFileResponse()
  ): Unit = {
    import com.github.tomakehurst.wiremock.client.WireMock._
    stubFor(
      post(urlEqualTo("/transfer-multiple-files"))
        .withRequestBody(equalToJson(Json.toJson(multiFileTransferRequest).toString(), true, true))
        .willReturn(
          aResponse()
            .withStatus(202)
            .withBody(Json.toJson(multiFileTransferResult).toString())
        )
    )
  }

  def givenFileTransmissionsMultipleFails(
    multiFileTransferRequest: MultiFileTransferRequest = multiFileRequest()
  ): Unit = {
    import com.github.tomakehurst.wiremock.client.WireMock._
    stubFor(
      post(urlEqualTo("/transfer-multiple-files"))
        .withRequestBody(equalToJson(Json.toJson(multiFileTransferRequest).toString(), true, true))
        .willReturn(
          aResponse()
            .withStatus(409)
        )
    )
  }

  val conversationId: String = java.util.UUID.randomUUID().toString

  def multiFileRequest(conversationId: String = conversationId): MultiFileTransferRequest = Json
    .parse(s"""{
              |    "conversationId": "$conversationId",
              |    "caseReferenceNumber": "NDRC000A00AB0ABCABC0AB0",
              |    "applicationName": "NDRC",
              |    "files": [
              |        {
              |            "upscanReference": "XYZ0123456789",
              |            "downloadUrl": "https://s3.amazonaws.com/bucket/9d9e1444-2555-422e-b251-44fd2e85530a",
              |            "fileName": "test1.jpeg",
              |            "fileMimeType": "image/jpeg",
              |            "fileSize": 12345,
              |            "checksum": "a38d7dd155b1ec9703e5f19f839922ad5a1b0aa4f255c6c2b03e61535997d75"
              |        }
              |    ],
              |    "callbackUrl":"https://foo.protected.mdtp/transfer-multiple-files/callback/NONCE"
              |}""".stripMargin)
    .as[MultiFileTransferRequest]

  def multiFileResponse(conversationId: String = conversationId): MultiFileTransferResult = Json
    .parse(s"""{
              |"conversationId": "$conversationId",
              |    "caseReferenceNumber": "NDRC000A00AB0ABCABC0AB0",
              |    "applicationName": "NDRC",
              |    "results":[
              |        {
              |            "upscanReference":"XYZ0123456789",
              |            "fileName": "test1.jpeg",
              |            "fileMimeType": "image/jpeg",
              |            "checksum":     "a38d7dd155b1ec9703e5f19f839922ad5a1b0aa4f255c6c2b03e61535997d75",
              |            "fileSize":12345,
              |            "success":true,
              |            "httpStatus":202,
              |            "transferredAt":"2021-07-11T12:53:46",
              |            "correlationId":"07b8090f-69c8-4708-bfc4-bf1731d4b4a8",
              |            "durationMillis": 1587
              |        }
              |    ],
              |    "totalDurationMillis": 1587
              |}""".stripMargin)
    .as[MultiFileTransferResult]

}
