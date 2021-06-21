package nationaldutyrepaymentcenter.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import nationaldutyrepaymentcenter.support.WireMockSupport
import org.scalatest.concurrent.Eventually
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.nationaldutyrepaymentcenter.services.NDRCAuditEvent.NDRCAuditEvent

trait DataStreamStubs extends Eventually {
  me: WireMockSupport =>

  override implicit val patienceConfig =
    PatienceConfig(scaled(Span(5, Seconds)), scaled(Span(500, Millis)))

  def verifyAuditRequestSent(
    count: Int,
    event: NDRCAuditEvent,
    details: JsObject,
    tags: Map[String, String] = Map.empty
  ): Unit = {
    val finalJson = s"""{
                       |  "auditSource": "national-duty-repayment-center",
                       |  "auditType": "$event",
                       |  "tags": ${Json.toJson(tags)},
                       |  "detail": ${Json.stringify(details)}
                       |}""".stripMargin

    eventually {
      verify(
        count,
        postRequestedFor(urlPathEqualTo(auditUrl))
          .withRequestBody(
            similarToJson(finalJson)
          )
      )
    }
  }

  def verifyAuditRequestNotSent(event: NDRCAuditEvent): Unit =
    eventually {
      verify(
        0,
        postRequestedFor(urlPathEqualTo(auditUrl))
          .withRequestBody(similarToJson(s"""{
          |  "auditSource": "national-duty-repayment-center",
          |  "auditType": "$event"
          |}"""))
      )
    }

  def givenAuditConnector(): Unit = {
    stubFor(post(urlPathEqualTo(auditUrl)).willReturn(aResponse().withStatus(204)))
    stubFor(post(urlPathEqualTo(auditUrl + "/merged")).willReturn(aResponse().withStatus(204)))
  }

  private def auditUrl = "/write/audit"

  private def similarToJson(value: String) = {
    equalToJson(value.stripMargin, true, true)
  }

}
