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

package uk.gov.hmrc.nationaldutyrepaymentcenter.models

sealed trait ClaimedUnderArticle

object ClaimedUnderArticle extends Enumerable.Implicits {

  case object OverchargedAmountsOfImportOrExportDuty extends WithName("117") with ClaimedUnderArticle

  case object ErrorByTheCompetentAuthorities extends WithName("119") with ClaimedUnderArticle

  case object Equity extends WithName("120") with ClaimedUnderArticle

  case object ErrorByCustoms extends WithName("048") with ClaimedUnderArticle

  case object LowerRateWasApplicable extends WithName("049") with ClaimedUnderArticle

  case object OverPaymentOfDutyOrVAT extends WithName("050") with ClaimedUnderArticle

  case object Rejected extends WithName("051") with ClaimedUnderArticle

  case object SpecialCircumstances extends WithName("052") with ClaimedUnderArticle

  case object WithdrawalOfCustomsDeclaration extends WithName("053") with ClaimedUnderArticle

  val values: Seq[ClaimedUnderArticle] = Seq(
    OverchargedAmountsOfImportOrExportDuty,
    ErrorByTheCompetentAuthorities,
    Equity,
    ErrorByCustoms,
    LowerRateWasApplicable,
    OverPaymentOfDutyOrVAT,
    Rejected,
    SpecialCircumstances,
    WithdrawalOfCustomsDeclaration
  )

  implicit val enumerable: Enumerable[ClaimedUnderArticle] =
    Enumerable(values.map(v => v.toString -> v): _*)
}
