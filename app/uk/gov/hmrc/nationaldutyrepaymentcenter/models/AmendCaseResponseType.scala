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

sealed trait AmendCaseResponseType

  object AmendCaseResponseType extends Enumerable.Implicits {

    case object SupportingDocuments extends WithName("supportingDocuments") with AmendCaseResponseType
    case object FurtherInformation extends WithName("furtherInformation") with AmendCaseResponseType


    val values: Seq[AmendCaseResponseType] = Seq(
      SupportingDocuments,
      FurtherInformation
    )

    implicit val enumerable: Enumerable[AmendCaseResponseType] =
      Enumerable(values.map(v => v.toString -> v): _*)

}