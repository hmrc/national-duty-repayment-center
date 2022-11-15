package nationaldutyrepaymentcenter.connectors

import nationaldutyrepaymentcenter.controllers.TestData
import nationaldutyrepaymentcenter.stubs.CreateCaseStubs
import nationaldutyrepaymentcenter.support.AppBaseISpec
import org.scalatest.RecoverMethods._
import play.api.Application
import uk.gov.hmrc.http._
import uk.gov.hmrc.nationaldutyrepaymentcenter.connectors.CreateCaseConnector
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.requests.{CreateClaimRequest, EISCreateCaseRequest}
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.responses.EISCreateCaseError.ErrorDetail
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.responses.{EISCreateCaseError, EISCreateCaseSuccess}

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.language.postfixOps

class CreateCaseConnectorISpec extends CreateCaseConnectorISpecSetup with CreateCaseStubs {

  "CreateCaseConnector" when {
    "submitClaim" should {
      "return EISCreateCaseSuccess if success" in {

        givenPegaCreateCaseRequestSucceeds("NDRC000A00AB0ABCABC0AB0")

        val result = await(connector.submitClaim(eisCreateCaseRequest, correlationId))
        result mustBe EISCreateCaseSuccess(
          "NDRC000A00AB0ABCABC0AB0",
          "2020-11-03T15:29:28.601Z",
          "Success",
          "Case created successfully"
        )
      }

      "return EISCreateCaseError if fails" in {

        givenPegaCreateCaseRequestFails(500, "errorCode", "error message", correlationId)

        val result = await(connector.submitClaim(eisCreateCaseRequest, correlationId))
        result mustBe EISCreateCaseError(
          ErrorDetail(
            Some(correlationId),
            Some("2020-11-03T15:29:28.601Z"),
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
          await(recoverToExceptionIf[GatewayTimeoutException](connector.submitClaim(
            eisCreateCaseRequest,
            correlationId
          )))

        ex mustBe an[GatewayTimeoutException]
        ex.responseCode mustBe 504
        ex.getMessage contains "/cpr/caserequest/ndrc/create/v1" mustBe true
      }

      "return Exception if EIS call fails unexpectedly" in {

        givenEISCallFailsUnexpectedly()

        val ex: Exception =
          await(recoverToExceptionIf[Exception](connector.submitClaim(eisCreateCaseRequest, correlationId)))

        ex mustBe an[Exception]
      }

      "return EISCreateCaseError if no body in response" in {

        givenPegaCreateCaseRequestRespondsWith403WithoutContent()

        val result = await(connector.submitClaim(eisCreateCaseRequest, correlationId))
        result mustBe EISCreateCaseError(
          ErrorDetail(None, None, Some("403"), Some("Error: empty response"), None, None)
        )
      }

      "retry if response is 429 (Too Many Requests)" in {

        givenPegaCreateCaseRequestSucceedsAfterTwoRetryResponses("NDRC000A00AB0ABCABC0AB0")

        val result = await(connector.submitClaim(eisCreateCaseRequest, correlationId))
        result mustBe EISCreateCaseSuccess(
          "NDRC000A00AB0ABCABC0AB0",
          "2020-11-03T15:29:28.601Z",
          "Success",
          "Case created successfully"
        )
      }
    }
  }
}

trait CreateCaseConnectorISpecSetup extends AppBaseISpec {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  override def fakeApplication(): Application = appBuilder.build()

  lazy val connector: CreateCaseConnector =
    app.injector.instanceOf[CreateCaseConnector]

  val createClaimRequest: CreateClaimRequest = TestData.testCreateCaseRequest(wireMockBaseUrlAsString)

  val correlationId: String = UUID.randomUUID().toString

  val eisCreateCaseRequest: EISCreateCaseRequest = EISCreateCaseRequest(
    AcknowledgementReference = correlationId.replace("-", "").takeRight(32),
    ApplicationType = "NDRC",
    OriginatingSystem = "Digital",
    Content = EISCreateCaseRequest.Content.from(createClaimRequest)
  )

}
