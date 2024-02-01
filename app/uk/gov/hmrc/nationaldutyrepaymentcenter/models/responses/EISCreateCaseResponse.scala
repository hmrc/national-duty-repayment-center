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

package uk.gov.hmrc.nationaldutyrepaymentcenter.models.responses

import java.util.concurrent.TimeUnit

import play.api.libs.json._
import uk.gov.hmrc.http.TooManyRequestException

import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Try}

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

}

object EISCreateCaseError {

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

  implicit lazy val reads: Reads[EISCreateCaseResponse] =
    Reads {
      case jsObject: JsObject if (jsObject \ "CaseID").isDefined =>
        EISCreateCaseSuccess.formats.reads(jsObject)
      case jsValue =>
        EISCreateCaseError.formats.reads(jsValue)
    }

  implicit lazy val writes: Writes[EISCreateCaseResponse] =
    new Writes[EISCreateCaseResponse] {

      override def writes(o: EISCreateCaseResponse): JsValue =
        o match {
          case s: EISCreateCaseSuccess =>
            EISCreateCaseSuccess.formats.writes(s)
          case e: EISCreateCaseError =>
            EISCreateCaseError.formats.writes(e)
        }

    }

  final def shouldRetry(response: Try[EISCreateCaseResponse]): Boolean =
    response match {
      case Failure(e: TooManyRequestException) => true
      case _                                   => false
    }

  final def errorMessage(response: Try[EISCreateCaseResponse]): String =
    response match {
      case Failure(e: TooManyRequestException) => "Too Many Requests"
      case _                                   => ""
    }

  final def delayInterval(response: Try[EISCreateCaseResponse]): Option[FiniteDuration] =
    response match {
      case Failure(e: TooManyRequestException) =>
        try Some(FiniteDuration(e.getMessage().toLong, TimeUnit.MILLISECONDS))
        catch {
          case e: NumberFormatException => None
        }
      case _ => None
    }

}
