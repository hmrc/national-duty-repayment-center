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

package base

import java.time.LocalDate

import org.scalatest.TryValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice._
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.Injector
import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.AmendCaseResponseType.FurtherInformation
import uk.gov.hmrc.nationaldutyrepaymentcenter.models._
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.requests.{AmendClaimRequest, CreateClaimRequest}

trait SpecBase extends PlaySpec with GuiceOneAppPerSuite with TryValues with ScalaFutures {

  val userAnswersId = "id"

  def injector: Injector = app.injector

  def messagesApi: MessagesApi = injector.instanceOf[MessagesApi]

  def fakeRequest = FakeRequest("", "")

  implicit lazy val messages: Messages = messagesApi.preferred(fakeRequest)

  val claimDetails = ClaimDetails(
    FormType = FormType("01"),
    CustomRegulationType = CustomRegulationType.UKCustomsCodeRegulation,
    ClaimedUnderArticle = None,
    ClaimedUnderRegulation = Some(ClaimedUnderRegulation.Rejected),
    Claimant = Claimant.RepresentativeOfTheImporter,
    ClaimType = ClaimType.Multiple,
    NoOfEntries = Some(NoOfEntries("10")),
    EntryDetails(EPU = "777", EntryNumber = "123456A", EntryDate = LocalDate.of(2020, 1, 1)),
    ClaimReason = ClaimReason.Preference,
    ClaimDescription = ClaimDescription("this is a claim description for £123"),
    DateReceived = LocalDate.of(2020, 8, 5),
    ClaimDate = LocalDate.of(2020, 8, 5),
    PayeeIndicator = PayeeIndicator.Importer,
    PaymentMethod = PaymentMethod.BACS,
    DeclarantRefNumber = "NA",
    DeclarantName = "DummyData"
  )

  val address = Address(
    AddressLine1 = "line 1",
    AddressLine2 = Some("line 2"),
    City = "city",
    Region = Some("region"),
    CountryCode = "GB",
    PostalCode = Some("ZZ111ZZ")
  )

  val userDetails = UserDetails(
    IsVATRegistered = "true",
    EORI = EORI("GB123456789123456"),
    Name = "Joe Bloggs",
    Address = address,
    TelephoneNumber = Some("1234567"),
    EmailAddress = Some("123@hotmail.com")
  )

  val bankDetails = AllBankDetails(
    AgentBankDetails = Some(BankDetails("account name", "123456", "12345678")),
    ImporterBankDetails = Some(BankDetails("account name", "123456", "12345678"))
  )

  val dutyTypeTaxList = Seq(
    DutyTypeTaxList(DutyType.Customs, "100.00", "50.00", "50.00"),
    DutyTypeTaxList(DutyType.Vat, "100.00", "50.00", "50.00"),
    DutyTypeTaxList(DutyType.Other, "100.00", "50.00", "50.00")
  )

  val documentList = Seq(
    DocumentList(DocumentUploadType.CopyOfC88, Some(DocumentDescription("this is a copy of c88"))),
    DocumentList(DocumentUploadType.Invoice, Some(DocumentDescription("this is an invoice"))),
    DocumentList(DocumentUploadType.PackingList, Some(DocumentDescription("this is a packing list")))
  )

  val dutyTypeTaxDetails = DutyTypeTaxDetails(dutyTypeTaxList)

  val createClaimRequest = CreateClaimRequest(
    Content(
      claimDetails,
      AgentDetails = Some(userDetails),
      ImporterDetails = userDetails,
      BankDetails = Some(bankDetails),
      DutyTypeTaxDetails = dutyTypeTaxDetails,
      DocumentList = documentList
    ),
    uploadedFiles = Nil
  )

  val amendClaimRequest = AmendClaimRequest(
    AmendContent(
      CaseID = "Risk-2507",
      Description = "update request for Risk-2507: Value £199.99",
      TypeOfAmendments = Seq(FurtherInformation)
    ),
    Nil
  )

  val amendJson = Json.obj(
    "Content" -> Json.obj("CaseID" -> "Risk-2507", "Description" -> "update request for Risk-2507: Value £199.99")
  )

  val json = Json.obj(
    "AcknowledgementReference" -> "123456",
    "ApplicationType"          -> "ndrc",
    "OriginatingSystem"        -> "Digital",
    "Content" -> Json.obj(
      "ClaimDetails" -> Json.obj(
        "FormType"             -> "01",
        "CustomRegulationType" -> "02",
        "ClaimedUnderArticle"  -> "051",
        "Claimant"             -> "02",
        "ClaimType"            -> "02",
        "NoOfEntries"          -> "10",
        "EPU"                  -> "777",
        "EntryNumber"          -> "123456A",
        "EntryDate"            -> "20200101",
        "ClaimReason"          -> "06",
        "ClaimDescription"     -> "this is a claim description for £123",
        "DateReceived"         -> "20200805",
        "ClaimDate"            -> "20200805",
        "PayeeIndicator"       -> "01",
        "PaymentMethod"        -> "02"
      ),
      "AgentDetails" -> Json.obj(
        "IsVATRegistered" -> "true",
        "EORI"            -> "GB123456789123456",
        "Name"            -> "Joe Bloggs",
        "Address" -> Json.obj(
          "AddressLine1"    -> "line 1",
          "AddressLine2"    -> "line 2",
          "City"            -> "city",
          "Region"          -> "region",
          "CountryCode"     -> "GB",
          "PostalCode"      -> "ZZ111ZZ",
          "TelephoneNumber" -> "12345678",
          "EmailAddress"    -> "example@example.com"
        )
      ),
      "ImporterDetails" -> Json.obj(
        "IsVATRegistered" -> "true",
        "EORI"            -> "GB123456789123456",
        "Name"            -> "Joe Bloggs",
        "Address" -> Json.obj(
          "AddressLine1"    -> "line 1",
          "AddressLine2"    -> "line 2",
          "City"            -> "city",
          "Region"          -> "region",
          "CountryCode"     -> "GB",
          "PostalCode"      -> "ZZ111ZZ",
          "TelephoneNumber" -> "12345678",
          "EmailAddress"    -> "example@example.com"
        )
      ),
      "BankDetails" -> Json.obj(
        "ImporterBankDetails" -> Json.obj(
          "AccountName"   -> "account name",
          "SortCode"      -> "123456",
          "AccountNumber" -> "12345678"
        ),
        "AgentBankDetails" -> Json.obj(
          "AccountName"   -> "account name",
          "SortCode"      -> "123456",
          "AccountNumber" -> "12345678"
        )
      ),
      "DutyTypeTaxDetails" -> Json.obj(
        "DutyTypeTaxList" -> Json.arr(
          Json.obj("Type" -> "01", "PaidAmount" -> "100.00", "DueAmount" -> "50.00", "ClaimAmount" -> "50.00"),
          Json.obj("Type" -> "02", "PaidAmount" -> "100.00", "DueAmount" -> "50.00", "ClaimAmount" -> "50.00"),
          Json.obj("Type" -> "03", "PaidAmount" -> "100.00", "DueAmount" -> "50.00", "ClaimAmount" -> "50.00")
        )
      ),
      "DocumentList" -> Json.arr(
        Json.obj("Type" -> "03", "Description" -> "this is a copy of c88"),
        Json.obj("Type" -> "01", "Description" -> "this is an invoice"),
        Json.obj("Type" -> "04", "Description" -> "this is a packing list")
      )
    )
  )

}
