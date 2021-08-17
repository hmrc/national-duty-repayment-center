package nationaldutyrepaymentcenter.controllers

import java.time.{Clock, ZoneId, ZonedDateTime}
import java.{util => ju}

import nationaldutyrepaymentcenter.stubs.{AmendCaseStubs, AuthStubs, DataStreamStubs, FileTransferStubs}
import nationaldutyrepaymentcenter.support.{JsonMatchers, ServerBaseISpec}
import org.mockito.Mockito.when
import org.scalatest.Suite
import org.scalatestplus.play.ServerProvider
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsObject, Json}
import play.api.libs.ws.WSClient
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.AmendCaseResponseType.{FurtherInformation, SupportingDocuments}
import uk.gov.hmrc.nationaldutyrepaymentcenter.models._
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.requests.AmendClaimRequest
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.responses.NDRCCaseResponse
import uk.gov.hmrc.nationaldutyrepaymentcenter.services.{NDRCAuditEvent, UUIDGenerator}

class NDRCAmendCaseISpec
    extends ServerBaseISpec with AuthStubs with AmendCaseStubs with JsonMatchers with FileTransferStubs
    with DataStreamStubs {

  this: Suite with ServerProvider =>

  val url = s"http://localhost:$port"

  override def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.auth.port"                     -> wireMockPort,
        "microservice.services.eis.createcaseapi.host"        -> wireMockHost,
        "microservice.services.eis.createcaseapi.port"        -> wireMockPort,
        "microservice.services.eis.createcaseapi.token"       -> "dummy-it-token",
        "microservice.services.eis.createcaseapi.environment" -> "it",
        "metrics.enabled"                                     -> true,
        "auditing.enabled"                                    -> true,
        "auditing.consumer.baseUri.host"                      -> wireMockHost,
        "auditing.consumer.baseUri.port"                      -> wireMockPort,
        "microservice.services.file-transfer.host"            -> wireMockHost,
        "microservice.services.file-transfer.port"            -> wireMockPort,
        "features.submitEORIOnAmend"                          -> true
      ).overrides(bind[Clock].toInstance(clock), bind[UUIDGenerator].toInstance(uuidGeneratorMock))

  override lazy val app = appBuilder.build()
  val wsClient          = app.injector.instanceOf[WSClient]
  val uuidGenerator     = app.injector.instanceOf[UUIDGenerator]

  "ClaimController" when {
    "POST /amend-case" should {
      "return 201 with CaseID as a result if successful PEGA API call" in {

        val correlationId = ju.UUID.randomUUID().toString()
        when(uuidGenerator.uuid).thenReturn(correlationId)

        val uf: UploadedFile = TestData.uploadedFiles(wireMockBaseUrlAsString).head
        val fileTransferRequest = MultiFileTransferRequest(
          correlationId,
          "Risk-2507",
          "NDRC",
          Seq(FileTransferData(uf.upscanReference, uf.downloadUrl, uf.checksum, uf.fileName, uf.fileMimeType, None)),
          Some("http://localhost:8451/file-transfer-callback")
        )

        givenAuthorised()
        givenAuditConnector()
        givenPegaAmendCaseRequestSucceeds(correlationId)
        givenFileTransmissionsMultipleSucceeds(multiFileTransferRequest = fileTransferRequest)

        val result = wsClient
          .url(s"$url/amend-case")
          .withHttpHeaders("X-Correlation-ID" -> correlationId)
          .post(
            Json.toJson(
              AmendTestData.testAmendCaseRequest(wireMockBaseUrlAsString, eori = Some(EORI("GB345356852357")))
            )
          )
          .futureValue

        result.status mustBe 201
        val response = result.json.as[NDRCCaseResponse]
        response.correlationId must be(correlationId)

        verifyAmendCaseSentWithEORI("GB345356852357")

        verifyAuditRequestSent(
          1,
          NDRCAuditEvent.UpdateCase,
          Json.obj("success" -> true) ++ Json.obj("EORI" -> "GB345356852357") ++ AmendTestData.createAuditEventRequest(
            wireMockBaseUrlAsString
          )
        )

        verifyFilesTransferredAudit(0)
      }

      "generate correlationId when none provided" in {

        val correlationId = uuidGenerator.uuid
        // ensure consistent UUID returned from `WithCorrelationId` trait
        when(uuidGenerator.uuid).thenReturn(correlationId)

        val uf = TestData.uploadedFiles(wireMockBaseUrlAsString).head
        val fileTransferRequest = MultiFileTransferRequest(
          correlationId,
          "Risk-2507",
          "NDRC",
          Seq(FileTransferData(uf.upscanReference, uf.downloadUrl, uf.checksum, uf.fileName, uf.fileMimeType, None)),
          Some("http://localhost:8451/file-transfer-callback")
        )

        givenAuthorised()
        givenAuditConnector()
        givenPegaAmendCaseRequestSucceeds(correlationId)
        givenFileTransmissionsMultipleSucceeds(multiFileTransferRequest = fileTransferRequest)

        val result = wsClient
          .url(s"$url/amend-case")
          // Do not set X-Correlation-ID on header
          .post(Json.toJson(AmendTestData.testAmendCaseRequest(wireMockBaseUrlAsString)))
          .futureValue

        result.status mustBe 201
        val response = result.json.as[NDRCCaseResponse]
        response.correlationId must be(correlationId)

        verifyAuditRequestSent(
          1,
          NDRCAuditEvent.UpdateCase,
          Json.obj("success" -> true) ++ AmendTestData.createAuditEventRequest(wireMockBaseUrlAsString)
        )

        verifyFilesTransferredAudit(0)
      }

      "return 201 with CaseID and fileResults should have error if file upload fails" in {

        val correlationId = ju.UUID.randomUUID().toString()
        when(uuidGenerator.uuid).thenReturn(correlationId)

        val uf = TestData.uploadedFiles(wireMockBaseUrlAsString).head
        val fileTransferRequest = MultiFileTransferRequest(
          correlationId,
          "Risk-2507",
          "NDRC",
          Seq(FileTransferData(uf.upscanReference, uf.downloadUrl, uf.checksum, uf.fileName, uf.fileMimeType, None)),
          Some("http://localhost:8451/file-transfer-callback")
        )

        givenAuthorised()
        givenAuditConnector()
        givenPegaAmendCaseRequestSucceeds(correlationId)
        givenFileTransmissionsMultipleFails(fileTransferRequest)

        val result = wsClient
          .url(s"$url/amend-case")
          .withHttpHeaders("X-Correlation-ID" -> correlationId)
          .post(Json.toJson(AmendTestData.testAmendCaseRequest(wireMockBaseUrlAsString)))
          .futureValue

        result.status mustBe 201
        val response = result.json.as[NDRCCaseResponse]
        response.correlationId must be(correlationId)

        verifyAuditRequestSent(
          1,
          NDRCAuditEvent.UpdateCase,
          Json.obj("success" -> true) ++ AmendTestData.createAuditEventRequest(wireMockBaseUrlAsString)
        )

        verifyFilesTransferFailedAudit(1, "TransferMultipleFiles failed")
      }

      "audit when payload validation fails" in {

        val correlationId = uuidGenerator.uuid
        when(uuidGenerator.uuid).thenReturn(correlationId)

        givenAuthorised()
        givenAuditConnector()
        givenPegaAmendCaseRequestFails(400, "400", "Something went wrong")

        val result = wsClient
          .url(s"$url/amend-case")
          .withHttpHeaders("X-Correlation-ID" -> correlationId)
          .post(Json.toJson(AmendTestData.testAmendCaseRequest(wireMockBaseUrlAsString)))
          .futureValue

        result.status mustBe 400

        verifyAuditRequestSent(
          1,
          NDRCAuditEvent.UpdateCase,
          Json.obj("success" -> false) ++ AmendTestData.createAuditEventRequestWhenError(wireMockBaseUrlAsString)
        )
        verifyFilesTransferredAudit(0)
      }

      "audit when too many requests received" in {

        val correlationId = uuidGenerator.uuid
        when(uuidGenerator.uuid).thenReturn(correlationId)

        givenAuthorised()
        givenAuditConnector()
        givenTooManyPegaAmendCaseRequest()

        val result = wsClient
          .url(s"$url/amend-case")
          .withHttpHeaders("X-Correlation-ID" -> correlationId)
          .post(Json.toJson(AmendTestData.testAmendCaseRequest(wireMockBaseUrlAsString)))
          .futureValue

        result.status mustBe 500

        verifyAuditRequestSent(1, NDRCAuditEvent.UpdateCase, Json.obj("errorMessage" -> "Failed after 3 retries"))
        verifyFilesTransferredAudit(0)
      }

      "not audit when authorisation fails" in {

        val correlationId = uuidGenerator.uuid
        when(uuidGenerator.uuid).thenReturn(correlationId)

        givenUnauthorisedWith("MissingBearerToken")
        givenAuditConnector()

        val result = wsClient
          .url(s"$url/amend-case")
          .withHttpHeaders("X-Correlation-ID" -> correlationId)
          .post(Json.toJson(AmendTestData.testAmendCaseRequest(wireMockBaseUrlAsString)))
          .futureValue

        result.status mustBe 401

        verifyAuditRequestNotSent(NDRCAuditEvent.UpdateCase)

        verifyFilesTransferredAudit(0)
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
    )
  )

  def testAmendCaseRequest(wireMockBaseUrlAsString: String, caseId: String = "Risk-2507", eori: Option[EORI] = None) =
    AmendClaimRequest(
      AmendContent(
        CaseID = caseId,
        Description = "update request for Risk-2507: Value £199.99",
        TypeOfAmendments = Seq(FurtherInformation, SupportingDocuments)
      ),
      uploadedFiles(wireMockBaseUrlAsString),
      eori
    )

  def createAuditEventRequest(baseUrl: String): JsObject =
    Json.obj(
      "caseId"      -> "Risk-2507",
      "description" -> "update request for Risk-2507: Value £199.99",
      "action"      -> "SendDocumentsAndFurtherInformation",
      "uploadedFiles" -> Json.arr(
        Json.obj(
          "upscanReference" -> "ref-123",
          "fileName"        -> "test1.jpeg",
          "checksum"        -> "f55a741917d512ab4c547ea97bdfdd8df72bed5fe51b6a248e0a5a0ae58061c8",
          "fileMimeType"    -> "image/jpeg",
          "uploadTimestamp" -> "2020-10-10T10:10:10Z[UTC]",
          "downloadUrl"     -> (baseUrl + "/bucket/test1.jpeg")
        )
      ),
      "numberOfFilesUploaded" -> 1
    )

  def createAuditEventRequestWhenError(baseUrl: String): JsObject =
    Json.obj(
      "caseId"      -> "Risk-2507",
      "description" -> "update request for Risk-2507: Value £199.99",
      "action"      -> "SendDocumentsAndFurtherInformation",
      "uploadedFiles" -> Json.arr(
        Json.obj(
          "upscanReference" -> "ref-123",
          "fileName"        -> "test1.jpeg",
          "checksum"        -> "f55a741917d512ab4c547ea97bdfdd8df72bed5fe51b6a248e0a5a0ae58061c8",
          "fileMimeType"    -> "image/jpeg",
          "uploadTimestamp" -> "2020-10-10T10:10:10Z[UTC]",
          "downloadUrl"     -> (baseUrl + "/bucket/test1.jpeg")
        )
      ),
      "numberOfFilesUploaded" -> 1,
      "errorCode"             -> "400",
      "errorMessage"          -> "Something went wrong"
    )

}
