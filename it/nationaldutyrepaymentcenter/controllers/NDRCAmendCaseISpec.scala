package nationaldutyrepaymentcenter.controllers

import java.time.LocalDateTime
import org.scalatest.Suite
import org.scalatestplus.play.ServerProvider
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.libs.json.JsObject

import java.{util => ju}
import nationaldutyrepaymentcenter.stubs.{AmendCaseStubs, AuthStubs}
import nationaldutyrepaymentcenter.support.{JsonMatchers, ServerBaseISpec}
import org.mockito.Mockito.when
import uk.gov.hmrc.nationaldutyrepaymentcenter.controllers.UUIDGenerator
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.AmendContent
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.requests.AmendClaimRequest

class NDRCAmendCaseISpec
  extends ServerBaseISpec with AuthStubs with AmendCaseStubs with JsonMatchers {

  this: Suite with ServerProvider =>

  val url = s"http://localhost:$port"

  val dateTime = LocalDateTime.now()

  val wsClient = app.injector.instanceOf[WSClient]

  "ClaimController" when {
    "POST /amend-case" should {
      "return 201 with CaseID as a result if successful PEGA API call" in {

        givenAuthorised()

        val correlationId = ju.UUID.randomUUID().toString()
        givenPegaAmendCaseRequestSucceeds(correlationId)

        val result = wsClient
          .url(s"$url/amend-case")
          .withHttpHeaders("X-Correlation-ID" -> correlationId)
          .post(Json.toJson(AmendTestData.testAmendCaseRequest))
          .futureValue

        result.status shouldBe 201
        result.json.as[JsObject] should (
          haveProperty[String]("correlationId", be(correlationId)) and
            haveProperty[String]("result", be("Risk-2507"))
          )
      }
    }
  }
}

object AmendTestData {

  val testAmendCaseRequest =
    AmendClaimRequest(
      AmendContent(
        CaseID = "Risk-2507",
        Description = "update request for Risk-2507"
      )
    )
}


