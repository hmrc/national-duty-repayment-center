/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.nationaldutyrepaymentcenter.connectors

import org.scalatestplus.play.PlaySpec
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json.{Json, Reads}
import uk.gov.hmrc.http.{HttpResponse, JsValidationException, TooManyRequestException, UpstreamErrorResponse}

class ReadSuccessOrFailureSpec extends PlaySpec {

  sealed trait Response
  case class SuccessResponse(value: String) extends Response
  case class FailureResponse(error: String) extends Response

  object Response {
    implicit val successReads: Reads[SuccessResponse] = Json.reads[SuccessResponse]
    implicit val failureReads: Reads[FailureResponse] = Json.reads[FailureResponse]
  }

  class TestReadSuccessOrFailure(implicit
    sReads: Reads[SuccessResponse],
    fReads: Reads[FailureResponse],
    mf: Manifest[Response]
  ) extends ReadSuccessOrFailure[Response, SuccessResponse, FailureResponse]((status, msg) =>
        FailureResponse(s"$status:$msg")
      )

  val reader = new TestReadSuccessOrFailure().readFromJsonSuccessOrFailure

  "ReadSuccessOrFailure" should {

    "parse a successful JSON response" in {
      val json     = Json.obj("value" -> "ok")
      val response = HttpResponse(200, json.toString(), Map(HeaderNames.CONTENT_TYPE -> Seq(MimeTypes.JSON)))
      val result   = reader.read("GET", "/test", response)
      result mustBe SuccessResponse("ok")
    }

    "parse a failure JSON response" in {
      val json     = Json.obj("error" -> "bad request")
      val response = HttpResponse(400, json.toString(), Map(HeaderNames.CONTENT_TYPE -> Seq(MimeTypes.JSON)))
      val result   = reader.read("POST", "/fail", response)
      result mustBe FailureResponse("bad request")
    }

    "fallback on empty body" in {
      val response = HttpResponse(500, "", Map(HeaderNames.CONTENT_TYPE -> Seq(MimeTypes.JSON)))
      val result   = reader.read("GET", "/empty", response)
      result mustBe FailureResponse("500:Error: empty response")
    }

    "fallback on missing content-type" in {
      val response = HttpResponse(200, """{"value":"ok"}""", Map.empty)
      val result   = reader.read("GET", "/nocontenttype", response)
      result mustBe FailureResponse("200:Error: missing content-type header")
    }

    "throw TooManyRequestException for 429" in {
      val response = HttpResponse(429, "", Map("Retry-After" -> Seq("10")))
      assertThrows[TooManyRequestException] {
        reader.read("GET", "/ratelimit", response)
      }
    }

    "throw UpstreamErrorResponse for unexpected content-type" in {
      val response = HttpResponse(200, "not json", Map(HeaderNames.CONTENT_TYPE -> Seq("text/plain")))
      assertThrows[UpstreamErrorResponse] {
        reader.read("GET", "/wrongtype", response)
      }
    }

    "throw JsValidationException for invalid JSON" in {
      val response = HttpResponse(200, """{"notvalue":"x"}""", Map(HeaderNames.CONTENT_TYPE -> Seq(MimeTypes.JSON)))
      assertThrows[JsValidationException] {
        reader.read("GET", "/badjson", response)
      }
    }
  }
}
