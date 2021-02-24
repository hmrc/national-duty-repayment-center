package nationaldutyrepaymentcenter.controllers

import nationaldutyrepaymentcenter.stubs.{AmendCaseStubs, AuthStubs, FileTransferStubs}
import nationaldutyrepaymentcenter.support.{JsonMatchers, ServerBaseISpec}
import org.mockito.Mockito.when
import org.scalatest.MustMatchers.convertToAnyMustWrapper
import org.scalatest.Suite
import org.scalatestplus.play.ServerProvider
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import uk.gov.hmrc.nationaldutyrepaymentcenter.controllers.UUIDGenerator
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.requests.AmendClaimRequest
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.responses.NDRCCreateCaseResponse
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.{AmendContent, FileTransferRequest, UploadedFile}

import java.time.{LocalDateTime, ZoneId, ZonedDateTime}
import java.{util => ju}

class NDRCAmendCaseISpec
  extends ServerBaseISpec with AuthStubs with AmendCaseStubs with JsonMatchers  with FileTransferStubs {

  this: Suite with ServerProvider =>

  val url = s"http://localhost:$port"

  val dateTime = LocalDateTime.now()

  val wsClient = app.injector.instanceOf[WSClient]
  val uuidGenerator = app.injector.instanceOf[UUIDGenerator]

  "ClaimController" when {
    "POST /amend-case" should {
      "return 201 with CaseID as a result if successful PEGA API call" in {

        givenAuthorised()
        val correlationId = ju.UUID.randomUUID().toString()
        when(uuidGenerator.uuid).thenReturn(correlationId)

        givenPegaAmendCaseRequestSucceeds(correlationId)

        val uf = TestData.uploadedFiles(wireMockBaseUrlAsString).head
        val fileTransferRequest = FileTransferRequest.fromUploadedFile("Risk-2507", correlationId, correlationId, "NDRC", 1, 1, uf)

        givenNdrcFileTransferSucceeds(fileTransferRequest)

        val result = wsClient
          .url(s"$url/amend-case")
          .withHttpHeaders("X-Correlation-ID" -> correlationId)
          .post(Json.toJson(AmendTestData.testAmendCaseRequest(wireMockBaseUrlAsString)))
          .futureValue

        result.status shouldBe 201
        val createResponse = result.json.as[NDRCCreateCaseResponse]
        createResponse.correlationId must be(correlationId)
        createResponse.result.get.fileTransferResults.size must be(1)
        createResponse.result.get.fileTransferResults.head.httpStatus must be(200)

      }
    }
  }
}

object AmendTestData {

  def uploadedFiles(wireMockBaseUrlAsString: String) = Seq(
    UploadedFile(
      "ref-123",
      downloadUrl = wireMockBaseUrlAsString + "/bucket/test1.jpeg",
      uploadTimestamp = ZonedDateTime.of(2020, 10, 10, 10, 10, 10, 0, ZoneId.of("UTC")),
      checksum = "f55a741917d512ab4c547ea97bdfdd8df72bed5fe51b6a248e0a5a0ae58061c8",
      fileName = "test1.jpeg",
      fileMimeType = "image/jpeg"
    ))

  def testAmendCaseRequest(wireMockBaseUrlAsString: String) =
    AmendClaimRequest(
      AmendContent(
        CaseID = "Risk-2507",
        Description = "update request for Risk-2507"
      ), uploadedFiles(wireMockBaseUrlAsString))
}


