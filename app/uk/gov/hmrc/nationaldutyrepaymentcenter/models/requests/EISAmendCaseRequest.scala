/*
 * Copyright 2023 HM Revenue & Customs
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

/**
 * Create specified case in the PEGA system. Based on spec "CPR01-1.0.0-EIS API Specification-Create Case from MDTP"
 *
 * @param AcknowledgementReference
 *   Unique id created at source after a form is saved Unique ID throughout the journey of a message-stored in CSG data
 *   records, may be passed to Decision Service, CSG records can be searched using this field etc.
 * @param ApplicationType
 *   Its key value to create the case for respective process.
 * @param OriginatingSystem
 *   “Digital” for all requests originating in Digital
 */
case class EISAmendCaseRequest(
  AcknowledgementReference: String,
  ApplicationType: String,
  OriginatingSystem: String,
  Content: EISAmendCaseRequest.Content
)

object EISAmendCaseRequest {
  implicit val formats: Format[EISAmendCaseRequest] = Json.format[EISAmendCaseRequest]

  case class Content(CaseID: String, Description: String)

  object Content {
    implicit val formats: Format[Content] = Json.format[Content]

    def from(request: AmendClaimRequest): Content =
      Content(CaseID = request.Content.CaseID, Description = request.Content.Description)

  }

}
