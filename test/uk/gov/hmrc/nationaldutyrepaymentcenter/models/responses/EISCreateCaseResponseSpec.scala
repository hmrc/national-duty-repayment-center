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
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.responses.EISCreateCaseError.ErrorDetail
import scala.util.Success

class EISCreateCaseResponseSpec extends SpecBase {

  "EISCreateCaseResponse" should {

    "create EISCreateCaseError from status and message" in {
      EISCreateCaseError.fromStatusAndMessage(404, "Not found") mustBe EISCreateCaseError(
        ErrorDetail(None, None, Some("404"), Some("Not found"))
      )
    }

    "parse a success response" in {

      val successJson = """{
                                |    "Status": "Success",
                                |    "StatusText": "Case created successfully",
                                |    "CaseID": "NDRCLHFWER34NKFJN4F3",
                                |    "ProcessingDate": "2020-11-03T15:29:28.601Z"
                                |}""".stripMargin

      val success: EISCreateCaseResponse =
        EISCreateCaseSuccess("NDRCLHFWER34NKFJN4F3", "2020-11-03T15:29:28.601Z", "Success", "Case created successfully")

      val successFromJson: JsValue            = Json.parse(successJson)
      val readResponse: EISCreateCaseResponse = Json.fromJson[EISCreateCaseResponse](successFromJson).get

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

      val error: EISCreateCaseResponse = EISCreateCaseError(
        ErrorDetail(Some("123123123"), Some("2020-11-03T15:29:28.601Z"), Some("ABC"), Some("ABC error"))
      )

      val errorFromJson: JsValue              = Json.parse(errorJson)
      val readResponse: EISCreateCaseResponse = Json.fromJson[EISCreateCaseResponse](errorFromJson).get

      readResponse mustBe error
      Json.toJson(error) mustBe Json.parse(errorJson)

    }

    "return empty error response when EISCreateCaseResponse is successful" in {
      val result = EISCreateCaseResponse.errorMessage(Success(EISCreateCaseSuccess(
        "Risk-2507",
        "2020-09-24T10:15:43.995Z",
        "Success",
        "Case Updated successfully"
      )))
      result mustBe ""
    }

    "delayInterval must return None when EISCreateCaseResponse is successful" in {
      val result = EISCreateCaseResponse.delayInterval(Success(EISCreateCaseSuccess(
        "Risk-2507",
        "2020-09-24T10:15:43.995Z",
        "Success",
        "Case Updated successfully"
      )))
      result mustBe None
    }
  }
}
