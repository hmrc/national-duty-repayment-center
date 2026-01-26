/*
 * Copyright 2026 HM Revenue & Customs
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

sealed trait ClaimedUnderRegulation

object ClaimedUnderRegulation extends Enumerable.Implicits {

  case object ErrorByCustoms extends WithName("048") with ClaimedUnderRegulation

  case object LowerRateWasApplicable extends WithName("049") with ClaimedUnderRegulation

  case object OverPaymentOfDutyOrVAT extends WithName("050") with ClaimedUnderRegulation

  case object Rejected extends WithName("051") with ClaimedUnderRegulation

  case object SpecialCircumstances extends WithName("052") with ClaimedUnderRegulation

  case object WithdrawalOfCustomsDeclaration extends WithName("053") with ClaimedUnderRegulation

  val values: Seq[ClaimedUnderRegulation] = Seq(
    ErrorByCustoms,
    LowerRateWasApplicable,
    OverPaymentOfDutyOrVAT,
    Rejected,
    SpecialCircumstances,
    WithdrawalOfCustomsDeclaration
  )

  implicit val enumerable: Enumerable[ClaimedUnderRegulation] =
    Enumerable(values.map(v => v.toString -> v): _*)

}
