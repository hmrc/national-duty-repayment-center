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

sealed trait ClaimedUnderArticleFE

object ClaimedUnderArticleFE extends Enumerable.Implicits {

  case object OverchargedAmountsOfImportOrExportDuty extends WithName("117") with ClaimedUnderArticleFE

  case object ErrorByTheCompetentAuthorities extends WithName("119") with ClaimedUnderArticleFE

  case object Equity extends WithName("120") with ClaimedUnderArticleFE

  val values: Seq[ClaimedUnderArticleFE] =
    Seq(OverchargedAmountsOfImportOrExportDuty, ErrorByTheCompetentAuthorities, Equity)

  implicit val enumerable: Enumerable[ClaimedUnderArticleFE] =
    Enumerable(values.map(v => v.toString -> v): _*)

}
