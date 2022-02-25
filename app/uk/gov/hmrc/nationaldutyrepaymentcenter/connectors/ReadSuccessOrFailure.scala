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

package uk.gov.hmrc.nationaldutyrepaymentcenter.connectors

import play.api.libs.json.{JsError, JsSuccess, Reads}
import play.mvc.Http.{HeaderNames, MimeTypes}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HttpException, HttpReads, HttpResponse, JsValidationException, TooManyRequestException, UpstreamErrorResponse}

import scala.util.Try

abstract class ReadSuccessOrFailure[A, S <: A: Reads, F <: A: Reads](fallback: (Int, String) => A)
  (implicit mf: Manifest[A]) {

  implicit val readFromJsonSuccessOrFailure: HttpReads[A] =
    HttpReads[HttpResponse]
      .flatMap { response =>
        response.status match {
          case 429 =>
            throw new TooManyRequestException(response.header("Retry-After").getOrElse(""))
          case 499 =>
            throw new HttpException(s"Timeout from EIS with status: ${response.status}", response.status)
          case status =>
            if (response.body.isEmpty)
              HttpReads.pure(fallback(status, "Error: empty response"))
            else
              response.header(HeaderNames.CONTENT_TYPE) match {
                case None =>
                  HttpReads.pure(fallback(status, "Error: missing content-type header"))

                case Some(MimeTypes.JSON) =>
                  if (status >= 200 && status < 300)
                    Try[HttpReads[A]](implicitly[Reads[S]].reads(response.json) match {
                      case JsSuccess(value, _) => HttpReads.pure(value)
                      case JsError(errors) =>
                        HttpReads.ask.map {
                          case (method, url, _) =>
                            throw new JsValidationException(method, url, mf.runtimeClass, errors.toString)
                        }
                    })
                      .fold(e => HttpReads.pure(fallback(status, e.getMessage)), identity)
                  else if (status >= 400)
                    Try[HttpReads[A]](implicitly[Reads[F]].reads(response.json) match {
                      case JsSuccess(value, _) => HttpReads.pure(value)
                      case JsError(errors) =>
                        HttpReads.ask.map {
                          case (method, url, _) =>
                            throw new JsValidationException(method, url, mf.runtimeClass, errors.toString)
                        }
                    })
                      .fold(e => HttpReads.pure(fallback(status, e.getMessage)), identity)
                  else
                    throw UpstreamErrorResponse(s"Unexpected response status $status", 500)

                case other =>
                  throw UpstreamErrorResponse(
                    s"Unexpected response type of status $status, expected application/json but got ${other
                      .getOrElse("none")} with body:\n${response.body}",
                    500
                  )
              }
        }

      }

}
