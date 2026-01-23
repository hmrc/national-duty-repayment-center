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

package uk.gov.hmrc.nationaldutyrepaymentcenter.models.requests

import base.SpecBase
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.{
  ClaimedUnderArticle,
  ClaimedUnderArticleFE,
  ClaimedUnderRegulation
}

class EISCreateCaseRequestSpec extends SpecBase {

  "EISCreateCaseRequest" should {

    "return correct ClaimedUnderArticle" when {

      def claimUnderArticleFE(article: ClaimedUnderArticleFE) = createClaimRequest.copy(Content =
        createClaimRequest.Content.copy(ClaimDetails = claimDetails.copy(ClaimedUnderArticle = Some(article)))
      )

      def claimUnderRegulationFE(article: ClaimedUnderRegulation) = createClaimRequest.copy(Content =
        createClaimRequest.Content.copy(ClaimDetails = claimDetails.copy(ClaimedUnderRegulation = Some(article)))
      )

      "ClaimedUnderArticleFE.Equity" in {

        EISCreateCaseRequest.Content.from(
          claimUnderArticleFE(ClaimedUnderArticleFE.Equity)
        ).ClaimDetails.ClaimedUnderArticle mustBe ClaimedUnderArticle.Equity
      }

      "ClaimedUnderArticleFE.ErrorByTheCompetentAuthorities" in {

        EISCreateCaseRequest.Content.from(
          claimUnderArticleFE(ClaimedUnderArticleFE.ErrorByTheCompetentAuthorities)
        ).ClaimDetails.ClaimedUnderArticle mustBe ClaimedUnderArticle.ErrorByTheCompetentAuthorities
      }

      "ClaimedUnderArticleFE.OverchargedAmountsOfImportOrExportDuty" in {

        EISCreateCaseRequest.Content.from(
          claimUnderArticleFE(ClaimedUnderArticleFE.OverchargedAmountsOfImportOrExportDuty)
        ).ClaimDetails.ClaimedUnderArticle mustBe ClaimedUnderArticle.OverchargedAmountsOfImportOrExportDuty
      }

      "ClaimedUnderRegulation.ErrorByCustoms" in {

        EISCreateCaseRequest.Content.from(
          claimUnderRegulationFE(ClaimedUnderRegulation.ErrorByCustoms)
        ).ClaimDetails.ClaimedUnderArticle mustBe ClaimedUnderArticle.ErrorByCustoms
      }

      "ClaimedUnderRegulation.LowerRateWasApplicable" in {

        EISCreateCaseRequest.Content.from(
          claimUnderRegulationFE(ClaimedUnderRegulation.LowerRateWasApplicable)
        ).ClaimDetails.ClaimedUnderArticle mustBe ClaimedUnderArticle.LowerRateWasApplicable
      }

      "ClaimedUnderRegulation.OverPaymentOfDutyOrVAT" in {

        EISCreateCaseRequest.Content.from(
          claimUnderRegulationFE(ClaimedUnderRegulation.OverPaymentOfDutyOrVAT)
        ).ClaimDetails.ClaimedUnderArticle mustBe ClaimedUnderArticle.OverPaymentOfDutyOrVAT
      }

      "ClaimedUnderRegulation.Rejected" in {

        EISCreateCaseRequest.Content.from(
          claimUnderRegulationFE(ClaimedUnderRegulation.Rejected)
        ).ClaimDetails.ClaimedUnderArticle mustBe ClaimedUnderArticle.Rejected
      }

      "ClaimedUnderRegulation.SpecialCircumstances" in {

        EISCreateCaseRequest.Content.from(
          claimUnderRegulationFE(ClaimedUnderRegulation.SpecialCircumstances)
        ).ClaimDetails.ClaimedUnderArticle mustBe ClaimedUnderArticle.SpecialCircumstances
      }

      "ClaimedUnderRegulation.WithdrawalOfCustomsDeclaration" in {

        EISCreateCaseRequest.Content.from(
          claimUnderRegulationFE(ClaimedUnderRegulation.WithdrawalOfCustomsDeclaration)
        ).ClaimDetails.ClaimedUnderArticle mustBe ClaimedUnderArticle.WithdrawalOfCustomsDeclaration
      }
    }

  }

}
