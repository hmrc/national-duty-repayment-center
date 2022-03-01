package nationaldutyrepaymentcenter.connectors

import nationaldutyrepaymentcenter.controllers.AmendTestData
import nationaldutyrepaymentcenter.stubs.AmendCaseStubs
import nationaldutyrepaymentcenter.support.AppBaseISpec
import org.scalatest.RecoverMethods.recoverToExceptionIf
import play.api.Application
import uk.gov.hmrc.http._
import uk.gov.hmrc.nationaldutyrepaymentcenter.connectors.AmendCaseConnector
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.requests.{AmendClaimRequest, EISAmendCaseRequest}
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.responses.EISAmendCaseError.ErrorDetail
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.responses.{EISAmendCaseError, EISAmendCaseSuccess}

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.language.postfixOps

class AmendCaseConnectorISpec extends AmendCaseConnectorISpecSetup with AmendCaseStubs {

  "AmendCaseConnector" when {
    "submitAmendClaim" should {
      "return EISCreateCaseSuccess if success" in {

        givenPegaAmendCaseRequestSucceeds(correlationId, caseId)

        val result = await(connector.submitAmendClaim(eisAmendCaseRequest, correlationId))
        result mustBe EISAmendCaseSuccess(caseId, "2020-09-24T10:15:43.995Z", "Success", "Case Updated successfully")
      }

      "return EISCreateCaseError if fails" in {

        givenPegaAmendCaseRequestFails(500, "errorCode", "error message", correlationId)

        val result = await(connector.submitAmendClaim(eisAmendCaseRequest, correlationId))
        result mustBe EISAmendCaseError(
          ErrorDetail(
            Some(correlationId),
            Some("2020-09-19T12:12:23.000Z"),
            Some("errorCode"),
            Some("error message"),
            None,
            None
          )
        )
      }

      "return GatewayTimeoutException and response code if EIS call times out" in {

        givenEISTimeout()

        implicit val defaultTimeout: FiniteDuration = 25 seconds

        val ex: GatewayTimeoutException =
          await(recoverToExceptionIf[GatewayTimeoutException](connector.submitAmendClaim(eisAmendCaseRequest, correlationId)))

        ex mustBe an[GatewayTimeoutException]
        ex.responseCode mustBe 504
        ex.getMessage contains "/cpr/caserequest/ndrc/update/v1" mustBe true
      }

      "return Exception if EIS call fails unexpectedly" in {

        givenEISCallFailsUnexpectedly()

        val ex: Exception =
          await(recoverToExceptionIf[Exception](connector.submitAmendClaim(eisAmendCaseRequest, correlationId)))

        ex mustBe an[Exception]
      }

      "return EISCreateCaseError if no body in response" in {

        givenPegaAmendCaseRequestRespondsWith403WithoutContent()

        val result = await(connector.submitAmendClaim(eisAmendCaseRequest, correlationId))
        result mustBe EISAmendCaseError(ErrorDetail(None, None, Some("403"), Some("Error: empty response"), None, None))
      }

      "retry if response is 429 (Too Many Requests)" in {

        givenPegaAmendCaseRequestSucceedsAfterTwoRetryResponses(caseId)

        val result = await(connector.submitAmendClaim(eisAmendCaseRequest, correlationId))
        result mustBe EISAmendCaseSuccess(caseId, "2020-11-03T15:29:28.601Z", "Success", "Case Updated successfully")
      }
    }
  }
}

trait AmendCaseConnectorISpecSetup extends AppBaseISpec {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  override def fakeApplication: Application = appBuilder.build()

  lazy val connector: AmendCaseConnector =
    app.injector.instanceOf[AmendCaseConnector]

  val caseId = "NDRC000A00AB0ABCABC0AB0"

  val amendClaimRequest: AmendClaimRequest = AmendTestData.testAmendCaseRequest(wireMockBaseUrlAsString, caseId)

  val correlationId: String = UUID.randomUUID().toString

  val eisAmendCaseRequest: EISAmendCaseRequest = EISAmendCaseRequest(
    AcknowledgementReference = correlationId.replace("-", "").takeRight(32),
    ApplicationType = "NDRC",
    OriginatingSystem = "Digital",
    Content = EISAmendCaseRequest.Content.from(amendClaimRequest)
  )

}
