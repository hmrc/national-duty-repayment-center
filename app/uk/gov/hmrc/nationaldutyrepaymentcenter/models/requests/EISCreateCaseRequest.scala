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

package uk.gov.hmrc.nationaldutyrepaymentcenter.models.requests

import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.{AllBankDetails, ClaimDetails, DocumentList, DutyTypeTaxDetails, EISUserDetails, UserDetails, UserName}

/**
 * Create specified case in the PEGA system.
 * Based on spec "CPR01-1.0.0-EIS API Specification-Create Case from MDTP"
 *
 * @param AcknowledgementReference Unique id created at source after a form is saved Unique ID throughout the journey of a message-stored in CSG data records, may be passed to Decision Service, CSG records can be searched using this field etc.
 * @param ApplicationType          Its key value to create the case for respective process.
 * @param OriginatingSystem        “Digital” for all requests originating in Digital
 */
case class EISCreateCaseRequest(
                                 AcknowledgementReference: String,
                                 ApplicationType: String,
                                 OriginatingSystem: String,
                                 Content: EISCreateCaseRequest.Content
                               )

object EISCreateCaseRequest {
  implicit val formats: Format[EISCreateCaseRequest] = Json.format[EISCreateCaseRequest]

  /**
   * @param ClaimDetails       see ClaimDetails structure.
   * @param AgentDetails       Agent/Representative of the importer Information (see UserDetails structure).
   * @param ImporterDetails    see UserDetails structure.
   * @param BankDetails        bank details of the payee required for BACS payments.
   * @param DutyTypeTaxDetails XXX.
   * @param DocumentList       CHIEF entry date in YYYYMMDD format.
   */

  case class Content(
                      ClaimDetails: ClaimDetails,
                      AgentDetails: Option[EISUserDetails],
                      ImporterDetails: EISUserDetails,
                      BankDetails: Option[AllBankDetails],
                      DutyTypeTaxDetails: DutyTypeTaxDetails,
                      DocumentList: Seq[DocumentList]
                    )

  object Content {
    implicit val formats: Format[Content] = Json.format[Content]

    def from(request: CreateClaimRequest): Content = {
      Content(
        ClaimDetails = request.Content.ClaimDetails,
        AgentDetails = request.Content.AgentDetails.isDefined match {
          case true  => Some(getAgentUserDetails(request))
          case _ => None
        },
        ImporterDetails = getImporterDetails(request),
        BankDetails = request.Content.BankDetails,
        DutyTypeTaxDetails = request.Content.DutyTypeTaxDetails,
        DocumentList = request.Content.DocumentList
      )
    }

    def getImporterDetails( request: CreateClaimRequest) : EISUserDetails = {
      val name = UserName(request.Content.ImporterDetails.Name.firstName,
          request.Content.ImporterDetails.Name.lastName)

      val fullName:String = (name.firstName + name.lastName).mkString("")

      EISUserDetails(
        IsVATRegistered = request.Content.ImporterDetails.IsVATRegistered,
        EORI = request.Content.ImporterDetails.EORI,
        Name =  fullName,
        Address = request.Content.ImporterDetails.Address
      )
    }


    def getAgentUserDetails(request: CreateClaimRequest): EISUserDetails =  {
      val name = UserName(request.Content.AgentDetails.get.Name.firstName,
          request.Content.ImporterDetails.Name.lastName)

      val fullName:String = (name.firstName + name.lastName).mkString("")

      EISUserDetails(
        IsVATRegistered = request.Content.AgentDetails.get.IsVATRegistered,
        EORI = request.Content.AgentDetails.get.EORI,
        Name =  fullName,
        Address = request.Content.AgentDetails.get.Address
      )
    }

  }

}
