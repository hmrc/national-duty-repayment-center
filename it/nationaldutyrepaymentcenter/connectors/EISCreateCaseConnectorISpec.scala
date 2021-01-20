package nationaldutyrepaymentcenter.connectors

import java.time.LocalDate

import play.api.Application
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.{Address, _}
import nationaldutyrepaymentcenter.stubs.CreateCaseStubs
import nationaldutyrepaymentcenter.support.AppBaseISpec
import uk.gov.hmrc.http._
import uk.gov.hmrc.nationaldutyrepaymentcenter.connectors.{CreateCaseConnector, PegaCaseError, PegaCaseSuccess}
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.requests.EISCreateCaseRequest

import scala.concurrent.ExecutionContext.Implicits.global

class EISCreateCaseConnectorISpec extends EISCreateCaseConnectorISpecSetup {

  /* "EISCreateCaseConnector" when {
     "createCase" should {
       "return case reference id if success" in {
         givenPegaCreateCaseRequestSucceeds()

         val request = testRequest

         val result = await(connector.submitClaim(request, correlationId))

         result shouldBe PegaCaseSuccess(
           "PCE201103470D2CC8K0NH3",
           "2020-11-03T15:29:28.601Z",
           "Success",
           "Case created successfully"
         )

       }

       "return error code and message if 500" in {
         givenPegaCreateCaseRequestFails(500, "500", "Foo Bar")

         val request = testRequest

         val result = await(connector.submitClaim(request, correlationId))

         result shouldBe PegaCaseError(errorDetail =
           PegaCaseError
             .ErrorDetail(
               correlationId = Some("123123123"),
               timestamp = Some("2020-11-03T15:29:28.601Z"),
               errorCode = Some("500"),
               errorMessage = Some("Foo Bar")
             )
         )
       }

       "return error code and message if 403" in {
         givenPegaCreateCaseRequestFails(403, "403", "Bar Foo")

         val request = testRequest

         val result = await(connector.submitClaim(request, correlationId))

         result shouldBe PegaCaseError(errorDetail =
           PegaCaseError
             .ErrorDetail(
               correlationId = Some("123123123"),
               timestamp = Some("2020-11-03T15:29:28.601Z"),
               errorCode = Some("403"),
               errorMessage = Some("Bar Foo")
             )
         )
       }
     }
   }*/

}

trait EISCreateCaseConnectorISpecSetup extends AppBaseISpec with CreateCaseStubs {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  override def fakeApplication: Application = appBuilder.build()

  lazy val connector: CreateCaseConnector =
    app.injector.instanceOf[CreateCaseConnector]

  val correlationId = java.util.UUID.randomUUID().toString()

  val testRequest = EISCreateCaseRequest(
    AcknowledgementReference = "XYZ123",
    ApplicationType = "NDRC",
    OriginatingSystem = "Digital",
    Content = EISCreateCaseRequest.Content(eisClaimDetails,
      AgentDetails = Some(eisUserDetails),
      ImporterDetails = eisUserDetails,
      BankDetails = Some(bankDetails),
      DutyTypeTaxDetails = dutyTypeTaxDetails,
      DocumentList = documentList)
  )

  val eisClaimDetails = EISClaimDetails(
    FormType = FormType("01"),
    CustomRegulationType = CustomRegulationType.UKCustomsCodeRegulation,
    ClaimedUnderArticle = ClaimedUnderArticle.OverchargedAmountsOfImportOrExportDuty,
    Claimant = Claimant.RepresentativeOfTheImporter,
    ClaimType = ClaimType.Multiple,
    NoOfEntries = Some(NoOfEntries("10")),
    EPU = "777",
    EntryNumber = "123456A",
    EntryDate = LocalDate.of(2020, 1, 1),
    ClaimReason = ClaimReason.Preference,
    ClaimDescription = ClaimDescription("this is a claim description"),
    DateReceived = LocalDate.of(2020, 8, 5),
    ClaimDate = LocalDate.of(2020, 8, 5),
    PayeeIndicator = PayeeIndicator.Importer,
    PaymentMethod = PaymentMethod.BACS,
    DeclarantRefNumber = "NA"
  )

  val eisAddress = EISAddress(AddressLine1 = "line 1",
    AddressLine2 = Some("line 2"),
    City = "city",
    Region = Some("region"),
    CountryCode = "GB",
    PostalCode = Some("ZZ111ZZ"),
    TelephoneNumber = Some("12345678"),
    EmailAddress = Some("example@example.com")
  )

  val eisUserDetails = EISUserDetails(IsVATRegistered = "true",
    EORI = EORI("GB123456789123456"),
    Name = "Joe Bloggs",
    Address = eisAddress
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
    DocumentList(DocumentUploadType.PackingList, Some(DocumentDescription("this is a packing list"))),
  )

  val dutyTypeTaxDetails = DutyTypeTaxDetails(dutyTypeTaxList)


}
