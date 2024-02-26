/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

  def verifyFilesTransferredAudit(times: Int) =
    eventually(verify(
      times,
      postRequestedFor(urlPathMatching(auditUrl))
        .withRequestBody(matchingJsonPath("$.auditType", containing("FilesTransferred")))
    ))

  def verifyFilesTransferSucceededAudit(times: Int) =
    eventually(verify(
      times,
      postRequestedFor(urlPathMatching(auditUrl))
        .withRequestBody(matchingJsonPath("$.auditType", containing("FilesTransferred")))
        .withRequestBody(matchingJsonPath("$.detail.fileTransferResults[*].success", containing("true")))
    ))

  def verifyFilesTransferFailedAudit(times: Int, containingError: String) =
    eventually(verify(
      times,
      postRequestedFor(urlPathMatching(auditUrl))
        .withRequestBody(matchingJsonPath("$.auditType", containing("FilesTransferred")))
        .withRequestBody(matchingJsonPath("$.detail.fileTransferResults[*].success", containing("false")))
        .withRequestBody(matchingJsonPath("$.detail.fileTransferResults[*].error", containing(containingError)))
    ))

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

  private def similarToJson(value: String) =
    equalToJson(value.stripMargin, true, true)

}
