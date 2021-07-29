/*
 * Copyright 2021 HM Revenue & Customs
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

import play.api.libs.json.{Format, Json}
import play.api.libs.json.Reads
import play.api.libs.json.Writes
import play.api.libs.json.JsObject
import play.api.libs.json.JsValue

sealed trait EISCreateCaseResponse

case class EISCreateCaseSuccess(CaseID: String, ProcessingDate: String, Status: String, StatusText: String)
    extends EISCreateCaseResponse

object EISCreateCaseSuccess {

  implicit val formats: Format[EISCreateCaseSuccess] =
    Json.format[EISCreateCaseSuccess]

}

case class EISCreateCaseError(errorDetail: EISCreateCaseError.ErrorDetail) extends EISCreateCaseResponse {

  def errorCode: Option[String]    = errorDetail.errorCode
  def errorMessage: Option[String] = errorDetail.errorMessage

  def isDuplicateCaseError: Boolean =
    errorDetail.errorMessage.exists(_.replace(" ", "").startsWith("999:"))

  def duplicateCaseID: Option[String] =
    errorDetail.errorMessage.map(_.replace(" ", "").drop(4))

}

object EISCreateCaseError {

  def apply(timestamp: String, correlationId: String, errorCode: String, errorMessage: String): EISCreateCaseError =
    EISCreateCaseError(errorDetail =
      ErrorDetail(Some(correlationId), Some(timestamp), Some(errorCode), Some(errorMessage))
    )

  def fromStatusAndMessage(status: Int, message: String): EISCreateCaseError =
    EISCreateCaseError(errorDetail = ErrorDetail(None, None, Some(status.toString), Some(message)))

  case class ErrorDetail(
    correlationId: Option[String] = None,
    timestamp: Option[String] = None,
    errorCode: Option[String] = None,
    errorMessage: Option[String] = None,
    source: Option[String] = None,
    sourceFaultDetail: Option[EISCreateCaseError.ErrorDetail.SourceFaultDetail] = None
  )

  object ErrorDetail {

    case class SourceFaultDetail(
      detail: Option[Seq[String]] = None,
      restFault: Option[JsObject] = None,
      soapFault: Option[JsObject] = None
    )

    object SourceFaultDetail {

      implicit val formats: Format[SourceFaultDetail] =
        Json.format[SourceFaultDetail]

    }

    implicit val formats: Format[ErrorDetail] =
      Json.format[ErrorDetail]

  }

  implicit val formats: Format[EISCreateCaseError] =
    Json.format[EISCreateCaseError]

}

object EISCreateCaseResponse {

  implicit def reads: Reads[EISCreateCaseResponse] =
    Reads {
      case jsObject: JsObject if (jsObject \ "CaseID").isDefined =>
        EISCreateCaseSuccess.formats.reads(jsObject)
      case jsValue =>
        EISCreateCaseError.formats.reads(jsValue)
    }

  implicit def writes: Writes[EISCreateCaseResponse] =
    new Writes[EISCreateCaseResponse] {

      override def writes(o: EISCreateCaseResponse): JsValue =
        o match {
          case s: EISCreateCaseSuccess =>
            EISCreateCaseSuccess.formats.writes(s)
          case e: EISCreateCaseError =>
            EISCreateCaseError.formats.writes(e)
        }

    }

}
