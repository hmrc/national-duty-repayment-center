package nationaldutyrepaymentcenter.stubs


import nationaldutyrepaymentcenter.support.WireMockSupport
import play.api.libs.json.Json
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.{FileTransferRequest, FileTransferResult}

import java.time.LocalDateTime

trait FileTransferStubs {
  me: WireMockSupport =>

  def givenNdrcFileTransferSucceeds(fileTransferRequest: FileTransferRequest): Unit = {
    import com.github.tomakehurst.wiremock.client.WireMock._
    val time = LocalDateTime.of(2020, 9, 9, 0, 0)
    stubFor(
      post(urlEqualTo("/transfer-file"))
        .withRequestBody(equalToJson(Json.toJson(fileTransferRequest).toString(), true, true ))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(Json.toJson(FileTransferResult(fileTransferRequest.correlationId.get, true, 200, time)).toString())
        )
    )
  }

  def givenNdrcFileTransferFails(fileTransferRequest: FileTransferRequest): Unit = {
    import com.github.tomakehurst.wiremock.client.WireMock._
    val time = LocalDateTime.of(2020, 9, 9, 0, 0)

    stubFor(
      post(urlEqualTo("/transfer-file"))
        .withRequestBody(equalToJson(Json.toJson(fileTransferRequest).toString(), true, true ))
        .willReturn(
          aResponse()
            .withStatus(409)
            .withBody(Json.toJson(FileTransferResult(fileTransferRequest.correlationId.get, true, 409, time)).toString())
        )
    )
  }
}
