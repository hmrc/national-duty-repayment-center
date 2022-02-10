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

package uk.gov.hmrc.nationaldutyrepaymentcenter.models

import java.time.LocalDate

import play.api.libs.json._

final case class ClaimDetails(
  FormType: FormType,
  CustomRegulationType: CustomRegulationType,
  ClaimedUnderArticle: Option[ClaimedUnderArticleFE],
  ClaimedUnderRegulation: Option[ClaimedUnderRegulation],
  Claimant: Claimant,
  ClaimType: ClaimType,
  NoOfEntries: Option[NoOfEntries],
  EntryDetails: EntryDetails,
  ClaimReason: ClaimReason,
  ClaimDescription: ClaimDescription,
  DateReceived: LocalDate,
  ClaimDate: LocalDate,
  PayeeIndicator: PayeeIndicator,
  PaymentMethod: PaymentMethod,
  DeclarantRefNumber: String,
  DeclarantName: String
)

object ClaimDetails {

  implicit val dateFormat: Format[LocalDate] = JsonFormatUtils.dateFormat

  implicit val format: OFormat[ClaimDetails] = Json.format[ClaimDetails]

}
