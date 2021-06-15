package nationaldutyrepaymentcenter.model

import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.AmendCaseResponseType.{FurtherInformation, SupportingDocuments}
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.AmendContent

class AmendContentSpec extends AnyWordSpec {

  "AmendContentSpec" should {

    "amendment type as 'Supportingdocuments'" in {
      val content = AmendContent("caseId", "desc", Seq(SupportingDocuments))
      content.selectedAmendments mustBe "SendDocuments"
    }

    "amendment type as 'Furtherinformation'" in {
      val content = AmendContent("caseId", "desc", Seq(FurtherInformation))
      content.selectedAmendments mustBe "SendFurtherInformation"
    }

    "amendment type as 'Furtherinformation' and 'Supportingdocuments'" in {
      val content = AmendContent("caseId", "desc", Seq(FurtherInformation, SupportingDocuments))
      content.selectedAmendments mustBe "SendDocumentsAndFurtherInformation"
    }
  }
}
