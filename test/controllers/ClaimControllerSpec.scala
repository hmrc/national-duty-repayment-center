/*
 * Copyright 2020 HM Revenue & Customs
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

package controllers

import java.time.{LocalDate, LocalDateTime}

import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.OptionValues._
import play.api.Application
import play.api.inject.bind
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, status, _}
import uk.gov.hmrc.nationaldutyrepaymentcenter.controllers.{ClaimController, routes}
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.{Address, AllBankDetails, BankDetails, ClaimAmount, ClaimDescription, ClaimDetails, ClaimReason, ClaimType, Claimant, ClaimedUnderArticle, Content, CustomRegulationType, DocumentDescription, DocumentList, DocumentUploadType, DueAmount, DutyType, DutyTypeTaxDetails, DutyTypeTaxList, EORI, EPU, EntryNumber, FormType, NoOfEntries, PaidAmount, PayeeIndicator, PaymentMethod, UserDetails, UserName, VRN}
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.requests.CreateClaimRequest
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.responses.{EISCreateCaseResponse, EISCreateCaseSuccess, NDRCCreateCaseResponse}
import uk.gov.hmrc.nationaldutyrepaymentcenter.services.ClaimService
import utils.UnitSpecBase

import scala.concurrent.Future

class ClaimControllerSpec extends UnitSpecBase {

  /*"POST /create-case" should {
    "return 200 if the service doesn't encounter any problems" in new Setup {

      when(mockService.createClaim(any(),any())(any())).thenReturn(Future.successful(EISCreateCaseSuccess("caseId", "20200101","status", "statusText")))

      private val app = application
      running(app) {
        val request = FakeRequest("POST", routes.ClaimController.submitClaim().url).withBody(json)

        val result = route(app, request).value
        status(result) shouldBe OK
        contentAsJson(result) shouldBe Json.toJson(NDRCCreateCaseResponse("correlationId", None, Some("caseId")))
      }
    }

  }*/

  trait Setup {

    val json = Json.obj(
      "AcknowledgementReference" -> "123456",
      "ApplicationType" -> "NDRC",
      "OriginatingSystem" -> "Digital",
      "Content" -> Json.obj(
        "ClaimDetails" -> Json.obj(
          "FormType" -> "01",
          "CustomRegulationType" -> "02",
          "ClaimedUnderArticle" -> "120",
          "Claimant" -> "02",
          "ClaimType" -> "02",
          "NoOfEntries" -> "10",
          "EPU" -> "777",
          "EntryNumber" -> "123456A",
          "EntryDate" -> "20200101",
          "ClaimReason" -> "06",
          "ClaimDescription" -> "this is a claim description",
          "DateReceived" -> "20200805",
          "ClaimDate" -> "20200805",
          "PayeeIndicator" -> "01",
          "PaymentMethod" -> "02",
        ),
        "AgentDetails" -> Json.obj(
          "VATNumber" -> "123456789",
          "EORI" -> "GB123456789123456",
          "Name" -> "Joe Bloggs",
          "Address" -> Json.obj(
            "AddressLine1" -> "line 1",
            "AddressLine2" -> "line 2",
            "City" -> "city",
            "Region" -> "region",
            "CountryCode" -> "GB",
            "PostalCode" -> "ZZ111ZZ",
            "TelephoneNumber" -> "12345678",
            "EmailAddress" -> "example@example.com"
          )
        ),
        "ImporterDetails" -> Json.obj(
          "VATNumber" -> "123456789",
          "EORI" -> "GB123456789123456",
          "Name" -> "Joe Bloggs",
          "Address" -> Json.obj(
            "AddressLine1" -> "line 1",
            "AddressLine2" -> "line 2",
            "City" -> "city",
            "Region" -> "region",
            "CountryCode" -> "GB",
            "PostalCode" -> "ZZ111ZZ",
            "TelephoneNumber" -> "12345678",
            "EmailAddress" -> "example@example.com"
          )
        ),
        "BankDetails" -> Json.obj(
          "ImporterBankDetails" -> Json.obj(
            "AccountName" -> "account name",
            "SortCode" -> "123456",
            "AccountNumber" -> "12345678"
          ),
          "AgentBankDetails" -> Json.obj(
            "AccountName" -> "account name",
            "SortCode" -> "123456",
            "AccountNumber" -> "12345678"
          )
        ),
        "DutyTypeTaxDetails" -> Json.obj(
          "DutyTypeTaxList" -> Json.arr(
            Json.obj(
              "Type" -> "01",
              "PaidAmount" -> "100.00",
              "DueAmount" -> "50.00",
              "ClaimAmount" -> "50.00"
            ),
            Json.obj(
              "Type" -> "02",
              "PaidAmount" -> "100.00",
              "DueAmount" -> "50.00",
              "ClaimAmount" -> "50.00"
            ),
            Json.obj(
              "Type" -> "03",
              "PaidAmount" -> "100.00",
              "DueAmount" -> "50.00",
              "ClaimAmount" -> "50.00"
            )
          )
        ),
        "DocumentList" -> Json.arr(
          Json.obj(
            "Type" -> "03",
            "Description" -> "this is a copy of c88"
          ),
          Json.obj(
            "Type" -> "01",
            "Description" -> "this is an invoice"
          ),
          Json.obj(
            "Type" -> "04",
            "Description" -> "this is a packing list"
          )
        )
      )
    )

    val mockService: ClaimService = mock[ClaimService]

    def application: Application =
      applicationBuilder.overrides(
        bind[ClaimService].toInstance(mockService)
      ).build()

    val testRequestBody: JsValue = Json.toJson(
      CreateClaimRequest(
        Content(claimDetails,
          AgentDetails = Some(userDetails),
          ImporterDetails = userDetails,
          BankDetails = Some(bankDetails),
          DutyTypeTaxDetails = dutyTypeTaxDetails,
          DocumentList = documentList)
      )
    )

    val claimDetails = ClaimDetails(
      FormType("01"),
      CustomRegulationType.UKCustomsCodeRegulation,
      ClaimedUnderArticle.OverchargedAmountsOfImportOrExportDuty,
      Claimant.RepresentativeOfTheImporter,
      ClaimType.Multiple,
      Some(NoOfEntries("10")),
      EPU("777"),
      EntryNumber("123456A"),
      LocalDate.of(2020,1,1),
      ClaimReason.Preference,
      ClaimDescription("this is a claim description"),
      LocalDate.of(2020,8,5),
      LocalDate.of(2020,8,5),
      PayeeIndicator.Importer,
      PaymentMethod.BACS
    )

    val address = Address(AddressLine1 = "line 1",
      AddressLine2 = Some("line 2"),
      City = "city",
      Region = "region",
      CountryCode = "GB",
      PostalCode = Some("ZZ111ZZ"),
      TelephoneNumber = Some("12345678"),
      EmailAddress = Some("example@example.com")
    )

    val userDetails = UserDetails(VATNumber = Some(VRN("123456789")),
      EORI = EORI("GB123456789123456"),
      Name = UserName("Joe Bloggs"),
      Address = address
    )

    val bankDetails = AllBankDetails(
      AgentBankDetails = BankDetails("account name", "123456", "12345678"),
      ImporterBankDetails = BankDetails("account name", "123456", "12345678")
    )

    val dutyTypeTaxList = Seq(
      DutyTypeTaxList(DutyType.Customs, Some(PaidAmount("100.00")), Some(DueAmount("50.00")), Some(ClaimAmount("50.00"))),
      DutyTypeTaxList(DutyType.Vat, Some(PaidAmount("100.00")), Some(DueAmount("50.00")), Some(ClaimAmount("50.00"))),
      DutyTypeTaxList(DutyType.Other, Some(PaidAmount("100.00")), Some(DueAmount("50.00")), Some(ClaimAmount("50.00")))
    )

    val documentList = Seq(
      DocumentList(DocumentUploadType.CopyOfC88, Some(DocumentDescription("this is a copy of c88"))),
      DocumentList(DocumentUploadType.Invoice, Some(DocumentDescription("this is an invoice"))),
      DocumentList(DocumentUploadType.PackingList, Some(DocumentDescription("this is a packing list"))),
    )

    val dutyTypeTaxDetails = DutyTypeTaxDetails(dutyTypeTaxList)

  }


}
