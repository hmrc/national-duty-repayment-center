package nationaldutyrepaymentcenter.stubs


import nationaldutyrepaymentcenter.support.WireMockSupport
import play.api.libs.json.Json
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.{FileTransferRequest, FileTransferResult}

import java.time.LocalDateTime

trait FileTransferStubs {
  me: WireMockSupport =>

  def givenNdrcFileTransferSucceeds(fileTransferRequest: FileTransferRequest): Unit = {
    import com.github.tomakehurst.wiremock.client.WireMock._
    stubFor(
      post(urlEqualTo("/transfer-file"))
        .withRequestBody(equalToJson(Json.toJson(fileTransferRequest).toString(), true, true ))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(Json.toJson(FileTransferResult(fileTransferRequest.correlationId.get, true, 200, LocalDateTime.now)).toString())
        )
    )
  }

  def givenNdrcFileTransferFails(fileTransferRequest: FileTransferRequest): Unit = {
    import com.github.tomakehurst.wiremock.client.WireMock._
    stubFor(
      post(urlEqualTo("/transfer-file"))
        .withRequestBody(equalToJson(Json.toJson(fileTransferRequest).toString(), true, true ))
        .willReturn(
          aResponse()
            .withStatus(409)
            .withBody(Json.toJson(FileTransferResult(fileTransferRequest.correlationId.get, true, 409, LocalDateTime.now)).toString())
        )
    )
  }
}
