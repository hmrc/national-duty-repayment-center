package nationaldutyrepaymentcenter.controllers

import java.time.LocalDateTime
import java.time.LocalDate

import org.scalatest.Suite
import org.scalatestplus.play.ServerProvider
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.libs.json.JsObject
import java.{util => ju}

import nationaldutyrepaymentcenter.stubs.{AuthStubs, CreateCaseStubs}
import nationaldutyrepaymentcenter.support.{JsonMatchers, ServerBaseISpec}
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.{Address, BankDetails, DocumentList, DutyTypeTaxDetails, _}
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.requests.CreateClaimRequest

class NDRCCreateCaseISpec
    extends ServerBaseISpec with AuthStubs with CreateCaseStubs with JsonMatchers {

  this: Suite with ServerProvider =>

  val url = s"http://localhost:$port"

  val dateTime = LocalDateTime.now()

  val wsClient = app.injector.instanceOf[WSClient]

  "ClaimController" when {
    "POST /create-case" should {
      "return 201 with CaseID as a result if successful PEGA API call" in {

        givenAuthorised()
        givenPegaCreateCaseRequestSucceeds()

        val correlationId = ju.UUID.randomUUID().toString()

        val result = wsClient
          .url(s"$url/create-case")
          .withHttpHeaders("X-Correlation-ID" -> correlationId)
          .post(Json.toJson(TestData.testCreateCaseRequest))
          .futureValue

        result.status shouldBe 201
        result.json.as[JsObject] should (
          haveProperty[String]("correlationId", be(correlationId)) and
            haveProperty[String]("result", be("PCE201103470D2CC8K0NH3"))
        )
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
    EntryDetails = EntryDetails("777", "123456A", LocalDate.of(2020,1,1)),
    ClaimReason = ClaimReason.Preference,
    ClaimDescription = ClaimDescription("this is a claim description"),
    DateReceived = LocalDate.of(2020,8,5),
    ClaimDate = LocalDate.of(2020,8,5),
    PayeeIndicator = PayeeIndicator.Importer,
    PaymentMethod = PaymentMethod.BACS
  )

  val address = Address(AddressLine1 = "line 1",
    AddressLine2 = Some("line 2"),
    City = "city",
    Region = "region",
    CountryCode = "GB",
    PostalCode = Some("ZZ111ZZ"),
    TelephoneNumber = Some("12345678"),
    EmailAddress = Some("example@example.com")
  )

  val userDetails = UserDetails(VATNumber = Some(VRN("12345678")),
    EORI = EORI("GB123456789123456"),
    Name = UserName("Joe Bloggs"),
    Address = address
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

  val dutyTypeTaxDetails = DutyTypeTaxDetails(dutyTypeTaxList)

  val testCreateCaseRequest =
    CreateClaimRequest(
      Content(claimDetails,
        AgentDetails = Some(userDetails),
        ImporterDetails = userDetails,
        BankDetails = Some(bankDetails),
        DutyTypeTaxDetails = dutyTypeTaxDetails,
        DocumentList = documentList)
    )

}


