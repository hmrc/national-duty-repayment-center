/*
 * Copyright 2022 HM Revenue & Customs
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

import java.util.concurrent.TimeUnit

import play.api.libs.json._
import uk.gov.hmrc.http.TooManyRequestException

import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Try}

sealed trait EISAmendCaseResponse

case class EISAmendCaseSuccess(CaseID: String, ProcessingDate: String, Status: String, StatusText: String)
    extends EISAmendCaseResponse

object EISAmendCaseSuccess {

  implicit val formats: Format[EISAmendCaseSuccess] =
    Json.format[EISAmendCaseSuccess]

}

case class EISAmendCaseError(errorDetail: EISAmendCaseError.ErrorDetail) extends EISAmendCaseResponse {

  def errorCode: Option[String] = errorDetail.errorCode

  def errorMessage: Option[String] = errorDetail.errorMessage

}

object EISAmendCaseError {

  def fromStatusAndMessage(status: Int, message: String): EISAmendCaseError =
    EISAmendCaseError(errorDetail = ErrorDetail(None, None, Some(status.toString), Some(message)))

  case class ErrorDetail(
    correlationId: Option[String] = None,
    timestamp: Option[String] = None,
    errorCode: Option[String] = None,
    errorMessage: Option[String] = None,
    source: Option[String] = None,
    sourceFaultDetail: Option[EISAmendCaseError.ErrorDetail.SourceFaultDetail] = None
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

  implicit val formats: Format[EISAmendCaseError] =
    Json.format[EISAmendCaseError]

}

object EISAmendCaseResponse {

  implicit lazy val reads: Reads[EISAmendCaseResponse] =
    Reads {
      case jsObject: JsObject if (jsObject \ "CaseID").isDefined =>
        EISAmendCaseSuccess.formats.reads(jsObject)
      case jsValue =>
        EISAmendCaseError.formats.reads(jsValue)
    }

  implicit lazy val writes: Writes[EISAmendCaseResponse] =
    new Writes[EISAmendCaseResponse] {

      override def writes(o: EISAmendCaseResponse): JsValue =
        o match {
          case s: EISAmendCaseSuccess =>
            EISAmendCaseSuccess.formats.writes(s)
          case e: EISAmendCaseError =>
            EISAmendCaseError.formats.writes(e)
        }

    }

  final def shouldRetry(response: Try[EISAmendCaseResponse]): Boolean =
    response match {
      case Failure(e: TooManyRequestException) => true
      case _                                   => false
    }

  final def errorMessage(response: Try[EISAmendCaseResponse]): String =
    response match {
      case Failure(e: TooManyRequestException) => "Too Many Requests"
      case _                                   => ""
    }

  final def delayInterval(response: Try[EISAmendCaseResponse]): Option[FiniteDuration] =
    response match {
      case Failure(e: TooManyRequestException) =>
        try Some(FiniteDuration(e.getMessage().toLong, TimeUnit.MILLISECONDS))
        catch {
          case e: NumberFormatException => None
        }
      case _ => None
    }

}
