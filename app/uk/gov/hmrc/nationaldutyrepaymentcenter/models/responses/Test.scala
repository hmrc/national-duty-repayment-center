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

import uk.gov.hmrc.http.TooManyRequestException

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Try}

trait Test {

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
