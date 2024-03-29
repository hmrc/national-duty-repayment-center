/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.nationaldutyrepaymentcenter.services

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

import com.google.inject.Singleton
import javax.inject.Inject
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json._
import play.api.mvc.Request
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.nationaldutyrepaymentcenter.models._
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.requests.{AmendClaimRequest, CreateClaimRequest}
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.responses.NDRCCaseResponse
import uk.gov.hmrc.play.audit.AuditExtensions._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

object NDRCAuditEvent extends Enumeration {
  type NDRCAuditEvent = Value
  val CreateCase, UpdateCase = Value
}

@Singleton
class AuditService @Inject() (val auditConnector: AuditConnector) {

  import AuditService._
  import NDRCAuditEvent._

  final def auditCreateCaseEvent(
    createRequest: CreateClaimRequest,
    createResponse: NDRCCaseResponse,
    eori: Option[EORI]
  )(implicit hc: HeaderCarrier, request: Request[Any], ec: ExecutionContext): Future[Unit] = {
    val details: JsValue =
      CreateCaseAuditEventDetails.from(createRequest, createResponse, eori)
    auditExtendedEvent(CreateCase, "create-case", details)
  }

  final def auditCreateCaseErrorEvent(
    createResponse: NDRCCaseResponse
  )(implicit hc: HeaderCarrier, request: Request[Any], ec: ExecutionContext): Future[Unit] = {
    val details: JsValue = pegaResponseToDetails(createResponse)
    auditExtendedEvent(CreateCase, "create-case", details)
  }

  final def auditUpdateCaseEvent(
    updateRequest: AmendClaimRequest,
    updateResponse: NDRCCaseResponse,
    eori: Option[EORI]
  )(implicit hc: HeaderCarrier, request: Request[Any], ec: ExecutionContext): Future[Unit] = {
    val details: JsValue =
      UpdateCaseAuditEventDetails.from(updateRequest, updateResponse, eori)
    auditExtendedEvent(UpdateCase, "update-case", details)
  }

  final def auditUpdateCaseErrorEvent(
    updateResponse: NDRCCaseResponse
  )(implicit hc: HeaderCarrier, request: Request[Any], ec: ExecutionContext): Future[Unit] = {
    val details: JsValue = pegaResponseToDetails(updateResponse)
    auditExtendedEvent(UpdateCase, "update-case", details)
  }

  final def auditFileTransferResults(
    result: MultiFileTransferResult
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Unit = auditConnector
    .sendExplicitAudit(
      "FilesTransferred",
      FileTransferAudit(result.caseReferenceNumber, result.conversationId, result.totalDurationMillis, result.results)
    )(hc, ec, Json.writes[FileTransferAudit])

  private def auditExtendedEvent(event: NDRCAuditEvent, transactionName: String, details: JsValue)(implicit
    hc: HeaderCarrier,
    request: Request[Any],
    ec: ExecutionContext
  ): Future[Unit] =
    sendExtended(createExtendedEvent(event, transactionName, details))

  private def createExtendedEvent(event: NDRCAuditEvent, transactionName: String, details: JsValue)(implicit
    hc: HeaderCarrier,
    request: Request[Any]
  ): ExtendedDataEvent = {
    val tags = hc.toAuditTags(transactionName, request.path)
    val extendedEvent = ExtendedDataEvent(
      auditSource = "national-duty-repayment-center",
      auditType = event.toString,
      tags = tags,
      detail = details
    )
    extendedEvent
  }

  private def sendExtended(events: ExtendedDataEvent*)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    Future {
      events.foreach { event =>
        Try(auditConnector.sendExtendedEvent(event))
      }
    }

}

object AuditService {

  case class CreateCaseAuditEventDetails(
    correlationId: String,
    success: Boolean,
    claimantEORI: Option[EORI],
    caseReferenceNumber: Option[String],
    claimDetails: ClaimDetails,
    agentDetails: Option[UserDetails],
    importerDetails: UserDetails,
    bankDetails: Option[AllBankDetails],
    documentTypeTaxDetails: DutyTypeTaxDetails,
    documentList: Seq[DocumentList],
    numberOfFilesUploaded: Int,
    uploadedFiles: Seq[UploadedFile]
  )

  object CreateCaseAuditEventDetails {

    def from(createRequest: CreateClaimRequest, createResponse: NDRCCaseResponse, eori: Option[EORI]): JsValue = {
      val requestDetails: JsObject =
        Json
          .toJson(
            CreateCaseAuditEventDetails(
              correlationId = createResponse.correlationId,
              success = true,
              claimantEORI = eori,
              caseReferenceNumber = createResponse.caseId,
              claimDetails = createRequest.Content.ClaimDetails,
              agentDetails = createRequest.Content.AgentDetails,
              importerDetails = createRequest.Content.ImporterDetails,
              bankDetails = createRequest.Content.BankDetails,
              documentTypeTaxDetails = createRequest.Content.DutyTypeTaxDetails,
              documentList = createRequest.Content.DocumentList,
              numberOfFilesUploaded = createRequest.uploadedFiles.size,
              uploadedFiles = createRequest.uploadedFiles
            )
          )
          .as[JsObject]

      if (createResponse.isSuccess) requestDetails
      else
        requestDetails ++ pegaResponseToDetails(createResponse)
    }

    implicit val formats: Format[CreateCaseAuditEventDetails] =
      Json.format[CreateCaseAuditEventDetails]

  }

  case class UpdateCaseAuditEventDetails(
    correlationId: String,
    success: Boolean,
    claimantEORI: Option[EORI],
    caseId: String,
    action: String,
    description: Option[String],
    numberOfFilesUploaded: Int,
    uploadedFiles: Seq[UploadedFile]
  )

  object UpdateCaseAuditEventDetails {

    def from(updateRequest: AmendClaimRequest, updateResponse: NDRCCaseResponse, eori: Option[EORI]): JsValue = {
      val requestDetails: JsObject = Json
        .toJson(
          UpdateCaseAuditEventDetails(
            correlationId = updateResponse.correlationId,
            success = true,
            claimantEORI = eori,
            caseId = updateRequest.Content.CaseID,
            action = updateRequest.Content.selectedAmendments,
            description = Option(updateRequest.Content.Description),
            numberOfFilesUploaded = updateRequest.uploadedFiles.size,
            uploadedFiles = updateRequest.uploadedFiles
          )
        )
        .as[JsObject]

      if (updateResponse.isSuccess) requestDetails
      else
        requestDetails ++ pegaResponseToDetails(updateResponse)
    }

    implicit val formats: Format[UpdateCaseAuditEventDetails] =
      Json.format[UpdateCaseAuditEventDetails]

  }

  def pegaResponseToDetails(caseResponse: NDRCCaseResponse): JsObject = Json.obj("success" -> caseResponse.isSuccess) ++
    (if (caseResponse.isSuccess)
       Json.obj("caseReferenceNumber" -> caseResponse.caseId)
     else Json.obj()) ++ caseResponse.error.map(e => Json.obj("errorCode" -> e.errorCode)).getOrElse(Json.obj()) ++
    caseResponse.error
      .flatMap(_.errorMessage)
      .map(m => Json.obj("errorMessage" -> m))
      .getOrElse(Json.obj())

}
