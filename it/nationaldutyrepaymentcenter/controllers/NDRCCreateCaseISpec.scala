package nationaldutyrepaymentcenter.controllers

import com.github.tomakehurst.wiremock.client.WireMock.{moreThanOrExactly, postRequestedFor, urlEqualTo}
import nationaldutyrepaymentcenter.controllers.TestData.verifyAuthorisationHasHappened
import nationaldutyrepaymentcenter.stubs.{AuthStubs, CreateCaseStubs, DataStreamStubs, FileTransferStubs}
import nationaldutyrepaymentcenter.support.{JsonMatchers, ServerBaseISpec}
import org.mockito.Mockito.when
import org.scalatest.MustMatchers.convertToAnyMustWrapper
import org.scalatest.Suite
import org.scalatestplus.play.ServerProvider
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsObject, Json}
import play.api.libs.ws.WSClient
import uk.gov.hmrc.nationaldutyrepaymentcenter.controllers.UUIDGenerator
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.requests.CreateClaimRequest
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.responses.NDRCCaseResponse
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.{Address, BankDetails, DocumentList, DutyTypeTaxDetails, _}
import uk.gov.hmrc.nationaldutyrepaymentcenter.services.{AuditService, NDRCAuditEvent}

import java.time.{Clock, Instant, LocalDate, LocalDateTime, ZoneId, ZonedDateTime}
import java.{util => ju}

class NDRCCreateCaseISpec
  extends ServerBaseISpec with AuthStubs with CreateCaseStubs with JsonMatchers with FileTransferStubs with DataStreamStubs {

  this: Suite with ServerProvider =>

  val url = s"http://localhost:$port"
  override val clock: Clock = Clock.fixed(Instant.parse("2020-09-09T10:15:30.00Z"), ZoneId.of("UTC"))

  override def appBuilder: GuiceApplicationBuilder = {
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.auth.port" -> wireMockPort,
        "microservice.services.eis.createcaseapi.host" -> wireMockHost,
        "microservice.services.eis.createcaseapi.port" -> wireMockPort,
        "microservice.services.eis.createcaseapi.token" -> "dummy-it-token",
        "microservice.services.eis.createcaseapi.environment" -> "it",
        "metrics.enabled" -> true,
        "auditing.enabled" -> true,
        "auditing.consumer.baseUri.host" -> wireMockHost,
        "auditing.consumer.baseUri.port" -> wireMockPort,
        "microservice.services.file-transfer.host" -> wireMockHost,
        "microservice.services.file-transfer.port" -> wireMockPort,
      )  .overrides(
      bind[Clock].toInstance(clock),
      bind[UUIDGenerator].toInstance(uuideGeneratorMock))
  }

  override lazy val app =  appBuilder.build()

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

        givenAuditConnector()
        givenAuthorised()
        givenPegaCreateCaseRequestSucceeds()
        givenNdrcFileTransferSucceeds(fileTransferRequest)

        val claimRequest = TestData.testCreateCaseRequest(wireMockBaseUrlAsString)
        val result = wsClient
          .url(s"$url/create-case")
          .withHttpHeaders("X-Correlation-ID" -> correlationId)
          .post(Json.toJson(claimRequest))
          .futureValue

        result.status shouldBe 201
        val createResponse = result.json.as[NDRCCaseResponse]
        createResponse.correlationId must be(correlationId)
        createResponse.result.get.fileTransferResults.size must be(1)
        createResponse.result.get.fileTransferResults.head.httpStatus must be(200)

        verifyAuthorisationHasHappened()

        verifyAuditRequestSent(
          1,
          NDRCAuditEvent.CreateCase,
          Json.obj(
            "success"             -> true,
            "caseReferenceNumber" -> "PCE201103470D2CC8K0NH3"
          ) ++ TestData.createRequestDetails(wireMockBaseUrlAsString, transferSuccess = true, transferredAt = createResponse.result.get.fileTransferResults.head.transferredAt.toString)
        )
      }
      "return 201 with CaseID and fileResults should have error if file upload fails" in {

        val correlationId = ju.UUID.randomUUID().toString()
        when(uuidGenerator.uuid).thenReturn(correlationId)

        val uf = TestData.uploadedFiles(wireMockBaseUrlAsString).head
        val fileTransferRequest = FileTransferRequest.fromUploadedFile("PCE201103470D2CC8K0NH3", correlationId, correlationId, "NDRC", 1, 1, uf)

        givenAuthorised()
        givenAuditConnector()
        givenPegaCreateCaseRequestSucceeds()
        givenNdrcFileTransferFails(fileTransferRequest)

        val result = wsClient
          .url(s"$url/create-case")
          .withHttpHeaders("X-Correlation-ID" -> correlationId)
          .post(Json.toJson(TestData.testCreateCaseRequest(wireMockBaseUrlAsString)))
          .futureValue

        result.status shouldBe 201
        val createResponse = result.json.as[NDRCCaseResponse]
        createResponse.correlationId must be(correlationId)
        createResponse.result.get.fileTransferResults.size must be(1)
        createResponse.result.get.fileTransferResults.head.httpStatus must be(409)

        verifyAuditRequestSent(
          1,
          NDRCAuditEvent.CreateCase,
          Json.obj(
            "success"             -> true,
            "caseReferenceNumber" -> "PCE201103470D2CC8K0NH3"
          ) ++ TestData.createRequestDetailsWithFileTransferFailures(wireMockBaseUrlAsString, transferSuccess = false, transferredAt = createResponse.result.get.fileTransferResults.head.transferredAt.toString)
        )
      }

      "audit when incoming validation fails" in {

        val correlationId = ju.UUID.randomUUID().toString()
        when(uuidGenerator.uuid).thenReturn(correlationId)

        givenAuthorised()
        givenAuditConnector()
        givenPegaCreateCaseRequestFails(400, "400", "Something went wrong")

        val result = wsClient
          .url(s"$url/create-case")
          .withHttpHeaders("X-Correlation-ID" -> correlationId)
          .post(Json.toJson(TestData.testCreateCaseRequest(wireMockBaseUrlAsString)))
          .futureValue

        result.status shouldBe 400

        verifyAuditRequestSent(
          1,
          NDRCAuditEvent.CreateCase,
          Json.obj(
            "success"             -> false
          ) ++ TestData.createAuditEventWhenError(wireMockBaseUrlAsString, transferSuccess = false)
        )
      }

    }
  }
}

object TestData {

  val claimDetails = ClaimDetails(
    FormType = FormType("01"),
    CustomRegulationType = CustomRegulationType.UKCustomsCodeRegulation,
    ClaimedUnderArticle = None,
    ClaimedUnderRegulation = Some(ClaimedUnderRegulation.Rejected),
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

  val invalidClaimDetails = ClaimDetails(
    FormType = FormType("0xxx1"),
    CustomRegulationType = CustomRegulationType.UKCustomsCodeRegulation,
    ClaimedUnderArticle = None,
    ClaimedUnderRegulation = Some(ClaimedUnderRegulation.Rejected),
    Claimant = Claimant.RepresentativeOfTheImporter,
    ClaimType = ClaimType.Multiple,
    NoOfEntries = Some(NoOfEntries("10")),
    EntryDetails = EntryDetails("777", "123456A", LocalDate.of(1000, 1, 1)),
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
    PostalCode = "ZZ111ZZ"
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

  def testInvalidCreateCaseRequest(wireMockBaseUrlAsString: String) =
    CreateClaimRequest(
      Content(
        ClaimDetails = invalidClaimDetails,
        AgentDetails = Some(userDetails),
        ImporterDetails = userDetails,
        BankDetails = Some(bankDetails),
        DutyTypeTaxDetails = dutyTypeTaxDetails,
        DocumentList = documentList),
      uploadedFiles(wireMockBaseUrlAsString)
    )

  def createRequestDetails(baseUrl: String, transferSuccess: Boolean, transferredAt: String): JsObject = {
    Json.obj(

      "claimDetails" -> Json.obj(
        "FormType" -> "01",
        "CustomRegulationType" -> "02",
        "ClaimedUnderRegulation" -> "051",
        "Claimant" -> "02",
        "NoOfEntries" -> "10",
        "ClaimType" -> "02",
        "EntryDetails" -> Json.obj(
          "EPU" -> "777",
          "EntryNumber" -> "123456A",
          "EntryDate" -> "20200101"
        ),
        "ClaimReason" -> "06",
        "ClaimDescription" -> "this is a claim description",
        "DateReceived" -> "20200805",
        "ClaimDate" -> "20200805",
        "PayeeIndicator" -> "01",
        "PaymentMethod" -> "02",
        "DeclarantRefNumber" -> "NA"
      ),
      "agentDetails" -> Json.obj(
        "IsVATRegistered" -> "true",
        "EORI" -> "GB123456789123456",
        "Name" -> Json.obj(
          "firstName" -> "Joe",
          "lastName" -> "Bloggs"
        ),
        "Address" -> Json.obj(
          "AddressLine1" -> "line 1",
          "AddressLine2" -> "line 2",
          "City" -> "city",
          "Region" -> "region",
          "CountryCode" -> "GB",
          "PostalCode" -> "ZZ111ZZ"
        ),
        "TelephoneNumber" -> "12345678",
        "EmailAddress" -> "example@example.com"
      ),
      "importerDetails" -> Json.obj(
        "IsVATRegistered" -> "true",
        "EORI" -> "GB123456789123456",
        "Name" -> Json.obj(
          "firstName" -> "Joe",
          "lastName" -> "Bloggs"
        ),
        "Address" -> Json.obj(
          "AddressLine1" -> "line 1",
          "AddressLine2" -> "line 2",
          "City" -> "city",
          "Region" -> "region",
          "CountryCode" -> "GB",
          "PostalCode" -> "ZZ111ZZ"
        ),
        "TelephoneNumber" -> "12345678",
        "EmailAddress" -> "example@example.com"
      ),

      "bankDetails" -> Json.obj(
        "ImporterBankDetails" -> Json.obj(
          "AccountName" -> "account name",
          "SortCode" -> "123456",
          "AccountNumber" -> "12345678"
        ),
        "AgentBankDetails" -> Json.obj(
          "AccountName" -> "account name",
          "SortCode" -> "123456",
          "AccountNumber" -> "12345678"
        )
      ),
      "documentTypeTaxDetails" -> Json.obj(
        "DutyTypeTaxList" -> Json.arr(
          Json.obj(
            "Type" -> "01",
            "PaidAmount" -> "100.00",
            "DueAmount" -> "50.00",
            "ClaimAmount" -> "50.00"
          ),
          Json.obj("Type" -> "02",
            "PaidAmount" -> "100.00",
            "DueAmount" -> "50.00",
            "ClaimAmount" -> "50.00"
          ),
          Json.obj(
            "Type" -> "03",
            "PaidAmount" -> "100.00",
            "DueAmount" -> "50.00",
            "ClaimAmount" -> "50.00"
          ))
      ),
      "documentList" -> Json.arr(
        Json.obj(
          "Type" -> "03",
          "Description" -> "this is a copy of c88"),
        Json.obj(
          "Type" -> "01",
          "Description" -> "this is an invoice"),
        Json.obj(
          "Type" -> "04",
          "Description" -> "this is a packing list")
      ),
      "uploadedFiles" -> Json.arr(
        Json.obj(
          "upscanReference" -> "ref-123",
          "fileName" -> "test1.jpeg",
          "checksum" -> "f55a741917d512ab4c547ea97bdfdd8df72bed5fe51b6a248e0a5a0ae58061c8",
          "fileMimeType" -> "image/jpeg",
          "uploadTimestamp" -> "2020-10-10T10:10:10Z[UTC]",
          "downloadUrl" -> (baseUrl + "/bucket/test1.jpeg"),
          "transferSuccess" -> transferSuccess,
          "transferHttpStatus" -> 200,
          "transferredAt" -> transferredAt,
        )
      ),
      "numberOfFilesUploaded" -> 1
    )
  }

  def createRequestDetailsWithFileTransferFailures(baseUrl: String, transferSuccess: Boolean, transferredAt: String): JsObject = {
   Json.obj(

      "claimDetails" -> Json.obj(
        "FormType" -> "01",
        "CustomRegulationType" -> "02",
        "ClaimedUnderRegulation" -> "051",
        "Claimant" -> "02",
        "NoOfEntries" -> "10",
        "ClaimType" -> "02",
        "EntryDetails" -> Json.obj(
          "EPU" -> "777",
          "EntryNumber" -> "123456A",
          "EntryDate" -> "20200101"
        ),
        "ClaimReason" -> "06",
        "ClaimDescription" -> "this is a claim description",
        "DateReceived" -> "20200805",
        "ClaimDate" -> "20200805",
        "PayeeIndicator" -> "01",
        "PaymentMethod" -> "02",
        "DeclarantRefNumber" -> "NA"
      ),
      "agentDetails" -> Json.obj(
        "IsVATRegistered" -> "true",
        "EORI" -> "GB123456789123456",
        "Name" -> Json.obj(
          "firstName" -> "Joe",
          "lastName" -> "Bloggs"
        ),
        "Address" -> Json.obj(
          "AddressLine1" -> "line 1",
          "AddressLine2" -> "line 2",
          "City" -> "city",
          "Region" -> "region",
          "CountryCode" -> "GB",
          "PostalCode" -> "ZZ111ZZ"
        ),
        "TelephoneNumber" -> "12345678",
        "EmailAddress" -> "example@example.com"
      ),
      "importerDetails" -> Json.obj(
        "IsVATRegistered" -> "true",
        "EORI" -> "GB123456789123456",
        "Name" -> Json.obj(
          "firstName" -> "Joe",
          "lastName" -> "Bloggs"
        ),
        "Address" -> Json.obj(
          "AddressLine1" -> "line 1",
          "AddressLine2" -> "line 2",
          "City" -> "city",
          "Region" -> "region",
          "CountryCode" -> "GB",
          "PostalCode" -> "ZZ111ZZ"
        ),
        "TelephoneNumber" -> "12345678",
        "EmailAddress" -> "example@example.com"
      ),

      "bankDetails" -> Json.obj(
        "ImporterBankDetails" -> Json.obj(
          "AccountName" -> "account name",
          "SortCode" -> "123456",
          "AccountNumber" -> "12345678"
        ),
        "AgentBankDetails" -> Json.obj(
          "AccountName" -> "account name",
          "SortCode" -> "123456",
          "AccountNumber" -> "12345678"
        )
      ),
      "documentTypeTaxDetails" -> Json.obj(
        "DutyTypeTaxList" -> Json.arr(
          Json.obj(
            "Type" -> "01",
            "PaidAmount" -> "100.00",
            "DueAmount" -> "50.00",
            "ClaimAmount" -> "50.00"
          ),
          Json.obj("Type" -> "02",
            "PaidAmount" -> "100.00",
            "DueAmount" -> "50.00",
            "ClaimAmount" -> "50.00"
          ),
          Json.obj(
            "Type" -> "03",
            "PaidAmount" -> "100.00",
            "DueAmount" -> "50.00",
            "ClaimAmount" -> "50.00"
          ))
      ),
      "documentList" -> Json.arr(
        Json.obj(
          "Type" -> "03",
          "Description" -> "this is a copy of c88"),
        Json.obj(
          "Type" -> "01",
          "Description" -> "this is an invoice"),
        Json.obj(
          "Type" -> "04",
          "Description" -> "this is a packing list")
      ),
      "uploadedFiles" -> Json.arr(
        Json.obj(
          "upscanReference" -> "ref-123",
          "fileName" -> "test1.jpeg",
          "checksum" -> "f55a741917d512ab4c547ea97bdfdd8df72bed5fe51b6a248e0a5a0ae58061c8",
          "fileMimeType" -> "image/jpeg",
          "uploadTimestamp" -> "2020-10-10T10:10:10Z[UTC]",
          "downloadUrl" -> (baseUrl + "/bucket/test1.jpeg"),
          "transferSuccess" -> transferSuccess,
          "transferHttpStatus" -> 409,
          "transferredAt" -> transferredAt,
        )
      ),
      "numberOfFilesUploaded" -> 1
    )
  }

  def createAuditEventWhenError(baseUrl: String, transferSuccess: Boolean): JsObject = {
    Json.obj(
      "claimDetails" -> Json.obj(
        "FormType" -> "01",
        "CustomRegulationType" -> "02",
        "ClaimedUnderRegulation" -> "051",
        "Claimant" -> "02",
        "NoOfEntries" -> "10",
        "ClaimType" -> "02",
        "EntryDetails" -> Json.obj(
          "EPU" -> "777",
          "EntryNumber" -> "123456A",
          "EntryDate" -> "20200101"
        ),
        "ClaimReason" -> "06",
        "ClaimDescription" -> "this is a claim description",
        "DateReceived" -> "20200805",
        "ClaimDate" -> "20200805",
        "PayeeIndicator" -> "01",
        "PaymentMethod" -> "02",
        "DeclarantRefNumber" -> "NA"
      ),
      "agentDetails" -> Json.obj(
        "IsVATRegistered" -> "true",
        "EORI" -> "GB123456789123456",
        "Name" -> Json.obj(
          "firstName" -> "Joe",
          "lastName" -> "Bloggs"
        ),
        "Address" -> Json.obj(
          "AddressLine1" -> "line 1",
          "AddressLine2" -> "line 2",
          "City" -> "city",
          "Region" -> "region",
          "CountryCode" -> "GB",
          "PostalCode" -> "ZZ111ZZ"
        ),
        "TelephoneNumber" -> "12345678",
        "EmailAddress" -> "example@example.com"
      ),
      "importerDetails" -> Json.obj(
        "IsVATRegistered" -> "true",
        "EORI" -> "GB123456789123456",
        "Name" -> Json.obj(
          "firstName" -> "Joe",
          "lastName" -> "Bloggs"
        ),
        "Address" -> Json.obj(
          "AddressLine1" -> "line 1",
          "AddressLine2" -> "line 2",
          "City" -> "city",
          "Region" -> "region",
          "CountryCode" -> "GB",
          "PostalCode" -> "ZZ111ZZ"
        ),
        "TelephoneNumber" -> "12345678",
        "EmailAddress" -> "example@example.com"
      ),

      "bankDetails" -> Json.obj(
        "ImporterBankDetails" -> Json.obj(
          "AccountName" -> "account name",
          "SortCode" -> "123456",
          "AccountNumber" -> "12345678"
        ),
        "AgentBankDetails" -> Json.obj(
          "AccountName" -> "account name",
          "SortCode" -> "123456",
          "AccountNumber" -> "12345678"
        )
      ),
      "documentTypeTaxDetails" -> Json.obj(
        "DutyTypeTaxList" -> Json.arr(
          Json.obj(
            "Type" -> "01",
            "PaidAmount" -> "100.00",
            "DueAmount" -> "50.00",
            "ClaimAmount" -> "50.00"
          ),
          Json.obj("Type" -> "02",
            "PaidAmount" -> "100.00",
            "DueAmount" -> "50.00",
            "ClaimAmount" -> "50.00"
          ),
          Json.obj(
            "Type" -> "03",
            "PaidAmount" -> "100.00",
            "DueAmount" -> "50.00",
            "ClaimAmount" -> "50.00"
          ))
      ),
      "documentList" -> Json.arr(
        Json.obj(
          "Type" -> "03",
          "Description" -> "this is a copy of c88"),
        Json.obj(
          "Type" -> "01",
          "Description" -> "this is an invoice"),
        Json.obj(
          "Type" -> "04",
          "Description" -> "this is a packing list")
      ),
      "uploadedFiles" -> Json.arr(
        Json.obj(
          "upscanReference" -> "ref-123",
          "fileName" -> "test1.jpeg",
          "checksum" -> "f55a741917d512ab4c547ea97bdfdd8df72bed5fe51b6a248e0a5a0ae58061c8",
          "fileMimeType" -> "image/jpeg",
          "uploadTimestamp" -> "2020-10-10T10:10:10Z[UTC]",
          "downloadUrl" -> (baseUrl + "/bucket/test1.jpeg"),
          "transferSuccess" -> transferSuccess
        )
      ),
      "numberOfFilesUploaded" -> 1,
      "errorCode" ->  "400",
      "errorMessage" -> "Something went wrong",
    )
  }


  def verifyAuthorisationHasHappened(): Unit = {
    import com.github.tomakehurst.wiremock.client.WireMock.verify
    verify(moreThanOrExactly(1), postRequestedFor(urlEqualTo("/auth/authorise")))
  }
}



