package nationaldutyrepaymentcenter.connectors

import play.api.Application
import nationaldutyrepaymentcenter.stubs.AmendCaseStubs
import nationaldutyrepaymentcenter.support.AppBaseISpec
import play.api.inject.bind
import uk.gov.hmrc.http._
import uk.gov.hmrc.nationaldutyrepaymentcenter.connectors.AmendCaseConnector
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.requests.EISAmendCaseRequest

import java.time.{Clock, Instant, ZoneId}

class EISAmendCaseConnectorISpec extends EISAmendCaseConnectorISpecSetup {

  /* "EISAmendCaseConnector" when {
     "createCase" should {
       "return case reference id if success" in {
         givenPegaAmendCaseRequestSucceeds()

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
         givenPegaAmendCaseRequestFails(500, "500", "Foo Bar")

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
         givenPegaAmendCaseRequestFails(403, "403", "Bar Foo")

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

trait EISAmendCaseConnectorISpecSetup extends AppBaseISpec with AmendCaseStubs {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  override def fakeApplication: Application = appBuilder.build()

  lazy val connector: AmendCaseConnector =
    app.injector.instanceOf[AmendCaseConnector]

  val correlationId = java.util.UUID.randomUUID().toString()

  val testRequest = EISAmendCaseRequest(
    AcknowledgementReference = "XYZ123",
    ApplicationType = "NDRC",
    OriginatingSystem = "Digital",
    Content = EISAmendCaseRequest.Content(CaseID = "Risk-2507",
      Description = "update request for Risk-2507")
  )
}
