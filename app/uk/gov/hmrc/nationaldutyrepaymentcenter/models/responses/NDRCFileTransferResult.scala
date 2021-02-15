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

import java.time.LocalDateTime
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.FileTransferResult


case class NDRCFileTransferResult(
                                 caseId: String,
                                 generatedAt: LocalDateTime,
                                 fileTransferResults: Seq[FileTransferResult]
                               )

object NDRCFileTransferResult {
  implicit val formats: Format[NDRCFileTransferResult] =
    Json.format[NDRCFileTransferResult]
}

case class NDRCFileTransferResponse(
                                       correlationId: String,
                                       error: Option[ApiError] = None,
                                       result: Option[NDRCFileTransferResult] = None
                                     ) {
  def isSuccess: Boolean = error.isEmpty && result.isDefined
  def isDuplicate: Boolean = error.exists(_.errorCode == "409")
}