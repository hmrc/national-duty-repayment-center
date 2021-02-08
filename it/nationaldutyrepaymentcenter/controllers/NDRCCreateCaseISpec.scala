package nationaldutyrepaymentcenter.controllers

import nationaldutyrepaymentcenter.stubs.{AuthStubs, CreateCaseStubs, FileTransferStubs}
import nationaldutyrepaymentcenter.support.{JsonMatchers, ServerBaseISpec}
import org.mockito.Mockito.when
import org.scalatest.MustMatchers.convertToAnyMustWrapper
import org.scalatest.Suite
import org.scalatestplus.play.ServerProvider
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import uk.gov.hmrc.nationaldutyrepaymentcenter.controllers.UUIDGenerator
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.requests.CreateClaimRequest
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.responses.NDRCCreateCaseResponse
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.{Address, BankDetails, DocumentList, DutyTypeTaxDetails, _}

import java.time.{LocalDate, LocalDateTime, ZoneId, ZonedDateTime}
import java.{util => ju}

class NDRCCreateCaseISpec
  extends ServerBaseISpec with AuthStubs with CreateCaseStubs with JsonMatchers with FileTransferStubs {

  this: Suite with ServerProvider =>

  val url = s"http://localhost:$port"

  val dateTime = LocalDateTime.now()

  val wsClient = app.injector.instanceOf[WSClient]
  val uuidGenerator = app.injector.instanceOf[UUIDGenerator]

  "ClaimController" when {
    "POST /create-case" should {
      "return 201 with CaseID as a result if successful PEGA API call" in {

        val correlationId = ju.UUID.randomUUID().toString()
        when(uuidGenerator.uuid).thenReturn(correlationId)

        val uf = TestData.uploadedFiles(wireMockBaseUrlAsString).head
        val fileTransferRequest = FileTransferRequest.fromUploadedFile("PCE201103470D2CC8K0NH3", correlationId, correlationId, "NDRC", 1, 1, uf)

        givenAuthorised()
        givenPegaCreateCaseRequestSucceeds()
        givenNdrcFileTransferSucceeds(fileTransferRequest)

        val result = wsClient
          .url(s"$url/create-case")
          .withHttpHeaders("X-Correlation-ID" -> correlationId)
          .post(Json.toJson(TestData.testCreateCaseRequest(wireMockBaseUrlAsString)))
          .futureValue

        result.status shouldBe 201
        val createResponse = result.json.as[NDRCCreateCaseResponse]
        createResponse.correlationId must be(correlationId)
        createResponse.result.get.fileTransferResults.size must be(1)
        createResponse.result.get.fileTransferResults.head.httpStatus must be(200)
      }
      "return 201 with CaseID and fileResults should have error if file upload fails" in {

        val correlationId = ju.UUID.randomUUID().toString()
        when(uuidGenerator.uuid).thenReturn(correlationId)

        val uf = TestData.uploadedFiles(wireMockBaseUrlAsString).head
        val fileTransferRequest = FileTransferRequest.fromUploadedFile("PCE201103470D2CC8K0NH3", correlationId, correlationId, "NDRC", 1, 1, uf)

        givenAuthorised()
        givenPegaCreateCaseRequestSucceeds()
        givenNdrcFileTransferFails(fileTransferRequest)

        val result = wsClient
          .url(s"$url/create-case")
          .withHttpHeaders("X-Correlation-ID" -> correlationId)
          .post(Json.toJson(TestData.testCreateCaseRequest(wireMockBaseUrlAsString)))
          .futureValue

        result.status shouldBe 201
        val createResponse = result.json.as[NDRCCreateCaseResponse]
        createResponse.correlationId must be(correlationId)
        createResponse.result.get.fileTransferResults.size must be(1)
        createResponse.result.get.fileTransferResults.head.httpStatus must be(409)
      }

    }
  }
}

object TestData {

  val claimDetails = ClaimDetails(
    FormType = FormType("01"),
    CustomRegulationType = CustomRegulationType.UKCustomsCodeRegulation,
    ClaimedUnderArticle = ClaimedUnderArticle.OverchargedAmountsOfImportOrExportDuty,
    Claimant = Claimant.RepresentativeOfTheImporter,
    ClaimType = ClaimType.Multiple,
    NoOfEntries = Some(NoOfEntries("10")),
    EntryDetails = EntryDetails("777", "123456A", LocalDate.of(2020, 1, 1)),
    ClaimReason = ClaimReason.Preference,
    ClaimDescription = ClaimDescription("this is a claim description"),
    DateReceived = LocalDate.of(2020, 8, 5),
    ClaimDate = LocalDate.of(2020, 8, 5),
    PayeeIndicator = PayeeIndicator.Importer,
    PaymentMethod = PaymentMethod.BACS,
    DeclarantRefNumber = "NA"
  )

  val address = Address(
    AddressLine1 = "line 1",
    AddressLine2 = Some("line 2"),
    City = "city",
    Region = Some("region"),
    CountryCode = "GB",
    PostalCode = Some("ZZ111ZZ")
  )

  val userDetails = UserDetails(
    IsVATRegistered = "true",
    EORI = EORI("GB123456789123456"),
    Name = UserName("Joe", "Bloggs"),
    Address = address,
    TelephoneNumber = Some("12345678"),
    EmailAddress = Some("example@example.com")
  )

  val bankDetails = AllBankDetails(
    AgentBankDetails = Some(BankDetails("account name", "123456", "12345678")),
    ImporterBankDetails = Some(BankDetails("account name", "123456", "12345678"))
  )

  val dutyTypeTaxList = Seq(
    DutyTypeTaxList(DutyType.Customs, "100.00", "50.00", "50.00"),
    DutyTypeTaxList(DutyType.Vat, "100.00", "50.00", "50.00"),
    DutyTypeTaxList(DutyType.Other, "100.00", "50.00", "50.00")
  )

  val documentList = Seq(
    DocumentList(DocumentUploadType.CopyOfC88, Some(DocumentDescription("this is a copy of c88"))),
    DocumentList(DocumentUploadType.Invoice, Some(DocumentDescription("this is an invoice"))),
    DocumentList(DocumentUploadType.PackingList, Some(DocumentDescription("this is a packing list")))
  )

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


  val dutyTypeTaxDetails = DutyTypeTaxDetails(dutyTypeTaxList)

  def testCreateCaseRequest(wireMockBaseUrlAsString: String) =
    CreateClaimRequest(
      Content(
        ClaimDetails = claimDetails,
        AgentDetails = Some(userDetails),
        ImporterDetails = userDetails,
        BankDetails = Some(bankDetails),
        DutyTypeTaxDetails = dutyTypeTaxDetails,
        DocumentList = documentList),
      uploadedFiles(wireMockBaseUrlAsString)
    )

}



