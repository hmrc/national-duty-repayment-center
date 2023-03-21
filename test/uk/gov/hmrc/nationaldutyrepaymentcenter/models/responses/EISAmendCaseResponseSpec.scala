/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.nationaldutyrepaymentcenter.models.responses

import base.SpecBase
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.responses.EISAmendCaseError.ErrorDetail
import scala.util.Success

class EISAmendCaseResponseSpec extends SpecBase {

  "EISAmendCaseResponse" should {

    "create EISAmendCaseError from status and message" in {
      EISAmendCaseError.fromStatusAndMessage(404, "Not found") mustBe EISAmendCaseError(
        ErrorDetail(None, None, Some("404"), Some("Not found"))
      )
    }

    "parse a success response" in {

      val successJson = """{
                          |    "Status": "Success",
                          |    "StatusText": "Case Updated successfully",
                          |    "CaseID": "Risk-2507",
                          |    "ProcessingDate": "2020-09-24T10:15:43.995Z"
                          |}""".stripMargin

      val success: EISAmendCaseResponse =
        EISAmendCaseSuccess("Risk-2507", "2020-09-24T10:15:43.995Z", "Success", "Case Updated successfully")

      val successFromJson: JsValue           = Json.parse(successJson)
      val readResponse: EISAmendCaseResponse = Json.fromJson[EISAmendCaseResponse](successFromJson).get

      readResponse mustBe success
      Json.toJson(success) mustBe Json.parse(successJson)
    }

    "parse an error response" in {

      val errorJson = s"""{"errorDetail":{
         |   "timestamp": "2020-11-03T15:29:28.601Z",
         |   "correlationId": "123123123",
         |   "errorCode": "ABC",
         |   "errorMessage": "ABC error"
         |}}""".stripMargin

      val error: EISAmendCaseResponse = EISAmendCaseError(
        ErrorDetail(Some("123123123"), Some("2020-11-03T15:29:28.601Z"), Some("ABC"), Some("ABC error"))
      )

      val errorFromJson: JsValue             = Json.parse(errorJson)
      val readResponse: EISAmendCaseResponse = Json.fromJson[EISAmendCaseResponse](errorFromJson).get

      readResponse mustBe error
      Json.toJson(error) mustBe Json.parse(errorJson)

    }

    "return empty error response when EISAmendCaseResponse is successful" in {
      val result = EISAmendCaseResponse.errorMessage(Success(EISAmendCaseSuccess(
        "Risk-2507",
        "2020-09-24T10:15:43.995Z",
        "Success",
        "Case Updated successfully"
      )))
      result mustBe ""
    }

    "delayInterval must return None when EIS response is successful" in {
      val result = EISAmendCaseResponse.delayInterval(Success(EISAmendCaseSuccess(
        "Risk-2507",
        "2020-09-24T10:15:43.995Z",
        "Success",
        "Case Updated successfully"
      )))
      result mustBe None
    }
  }
}
