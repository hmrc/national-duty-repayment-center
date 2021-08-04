package nationaldutyrepaymentcenter.connectors

import java.util.UUID

import nationaldutyrepaymentcenter.controllers.AmendTestData
import nationaldutyrepaymentcenter.stubs.AmendCaseStubs
import nationaldutyrepaymentcenter.support.AppBaseISpec
import play.api.Application
import uk.gov.hmrc.http._
import uk.gov.hmrc.nationaldutyrepaymentcenter.connectors.AmendCaseConnector
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.requests.{AmendClaimRequest, EISAmendCaseRequest}
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.responses.EISAmendCaseError.ErrorDetail
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.responses.{EISAmendCaseError, EISAmendCaseSuccess}

class AmendCaseConnectorISpec extends AmendCaseConnectorISpecSetup with AmendCaseStubs {

  "AmendCaseConnector" when {
    "submitAmendClaim" should {
      "return EISCreateCaseSuccess if success" in {

        givenPegaAmendCaseRequestSucceeds(correlationId, "NDRC000A00AB0ABCABC0AB0")

        val result = await(connector.submitAmendClaim(eisAmendCaseRequest, correlationId))
        result mustBe EISAmendCaseSuccess(
          "NDRC000A00AB0ABCABC0AB0",
          "2020-09-24T10:15:43.995Z",
          "Success",
          "Case Updated successfully"
        )
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

      "return EISCreateCaseError if no body in response" in {

        givenPegaAmendCaseRequestRespondsWith403WithoutContent()

        val result = await(connector.submitAmendClaim(eisAmendCaseRequest, correlationId))
        result mustBe EISAmendCaseError(ErrorDetail(None, None, Some("403"), Some("Error: empty response"), None, None))
      }

      "retry if response is 429 (Too Many Requests)" in {

        givenPegaAmendCaseRequestSucceedsAfterTwoRetryResponses("NDRC000A00AB0ABCABC0AB0")

        val result = await(connector.submitAmendClaim(eisAmendCaseRequest, correlationId))
        result mustBe EISAmendCaseSuccess(
          "NDRC000A00AB0ABCABC0AB0",
          "2020-11-03T15:29:28.601Z",
          "Success",
          "Case Updated successfully"
        )
      }
    }
  }
}

trait AmendCaseConnectorISpecSetup extends AppBaseISpec {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  override def fakeApplication: Application = appBuilder.build()

  lazy val connector: AmendCaseConnector =
    app.injector.instanceOf[AmendCaseConnector]

  val amendClaimRequest: AmendClaimRequest = AmendTestData.testAmendCaseRequest(wireMockBaseUrlAsString)

  val correlationId = UUID.randomUUID().toString

  val eisAmendCaseRequest = EISAmendCaseRequest(
    AcknowledgementReference = correlationId.replace("-", "").takeRight(32),
    ApplicationType = "NDRC",
    OriginatingSystem = "Digital",
    Content = EISAmendCaseRequest.Content.from(amendClaimRequest)
  )

}
