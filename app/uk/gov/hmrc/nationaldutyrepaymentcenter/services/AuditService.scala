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

import com.fasterxml.jackson.annotation.JsonFormat
import com.google.inject.Singleton
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json._
import play.api.mvc.Request
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.AmendCaseResponseType.{FurtherInformation, SupportingDocuments}
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.requests.{AmendClaimRequest, CreateClaimRequest}
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.responses.NDRCCaseResponse
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.{AllBankDetails, AmendCaseResponseType, ClaimDetails, DocumentList, DutyTypeTaxDetails, FileTransferAudit, FileTransferResult, UploadedFile, UserDetails}
import uk.gov.hmrc.play.audit.AuditExtensions._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import javax.inject.Inject
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

  final def auditCreateCaseEvent(createRequest: CreateClaimRequest)(
    createResponse: NDRCCaseResponse
  )(implicit hc: HeaderCarrier, request: Request[Any], ec: ExecutionContext): Future[Unit] = {
    val details: JsValue =
      CreateCaseAuditEventDetails.from(createRequest, createResponse)
    auditExtendedEvent(CreateCase, "create-case", details)
  }

  final def auditCreateCaseErrorEvent(
                                       createResponse: NDRCCaseResponse
                                     )(implicit hc: HeaderCarrier, request: Request[Any], ec: ExecutionContext): Future[Unit] = {
    val details: JsValue = pegaResponseToDetails(createResponse)
    auditExtendedEvent(CreateCase, "create-case", details)
  }

  final def auditUpdateCaseEvent(updateRequest: AmendClaimRequest)(
    updateResponse: NDRCCaseResponse
  )(implicit hc: HeaderCarrier, request: Request[Any], ec: ExecutionContext): Future[Unit] = {
    val details: JsValue =
      UpdateCaseAuditEventDetails.from(updateRequest, updateResponse)
    auditExtendedEvent(UpdateCase, "update-case", details)
  }

  final def auditUpdateCaseErrorEvent(
                                       updateResponse: NDRCCaseResponse
                                     )(implicit hc: HeaderCarrier, request: Request[Any], ec: ExecutionContext): Future[Unit] = {
    val details: JsValue = pegaResponseToDetails(updateResponse)
    auditExtendedEvent(UpdateCase, "update-case", details)
  }

  private def auditExtendedEvent(
                                  event: NDRCAuditEvent,
                                  transactionName: String,
                                  details: JsValue
                                )(implicit hc: HeaderCarrier, request: Request[Any], ec: ExecutionContext): Future[Unit] =
    sendExtended(createExtendedEvent(event, transactionName, details))

  private def createExtendedEvent(
                                   event: NDRCAuditEvent,
                                   transactionName: String,
                                   details: JsValue
                                 )(implicit hc: HeaderCarrier, request: Request[Any], ec: ExecutionContext): ExtendedDataEvent = {
    val tags = hc.toAuditTags(transactionName, request.path)
    implicit val writes = Json.format[ExtendedDataEvent]
    val extendedEvent = ExtendedDataEvent(
      auditSource = "national-duty-repayment-center",
      auditType = event.toString,
      tags = tags,
      detail = details
    )
    extendedEvent
  }

  private def sendExtended(
                            events: ExtendedDataEvent*
                          )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    Future {
      events.foreach { event =>
        Try(auditConnector.sendExtendedEvent(event))
      }
    }

}

object AuditService {

  case class CreateCaseAuditEventDetails(
                                          success: Boolean,
                                          caseReferenceNumber: Option[String],
                                          claimDetails: ClaimDetails,
                                          agentDetails: Option[UserDetails],
                                          importerDetails: UserDetails,
                                          bankDetails: Option[AllBankDetails],
                                          documentTypeTaxDetails: DutyTypeTaxDetails,
                                          documentList: Seq[DocumentList],
                                          numberOfFilesUploaded: Int,
                                          uploadedFiles: Seq[FileTransferAudit]
                                        )

  object CreateCaseAuditEventDetails {

    def from(
              createRequest: CreateClaimRequest,
              createResponse: NDRCCaseResponse
            ): JsValue = {
      val requestDetails: JsObject = Json
        .toJson(

              CreateCaseAuditEventDetails(
                success = true,
                caseReferenceNumber = createResponse.result.map(_.caseId),
                claimDetails = createRequest.Content.ClaimDetails,
                agentDetails = createRequest.Content.AgentDetails,
                importerDetails = createRequest.Content.ImporterDetails,
                bankDetails = createRequest.Content.BankDetails,
                documentTypeTaxDetails = createRequest.Content.DutyTypeTaxDetails,
                documentList = createRequest.Content.DocumentList,
                numberOfFilesUploaded = createRequest.uploadedFiles.size,
                uploadedFiles = combineFileUploadAndTransferResults(
                  createRequest.uploadedFiles,
                  createResponse.result.map(_.fileTransferResults)
                )
              ))
        .as[JsObject]

      if (createResponse.result.isDefined) requestDetails
      else
        (requestDetails ++ pegaResponseToDetails(createResponse))
    }

    implicit val formats: Format[CreateCaseAuditEventDetails] =
      Json.format[CreateCaseAuditEventDetails]
  }

  case class UpdateCaseAuditEventDetails(
                                          success: Boolean,
                                          caseId: String,
                                          action: String,
                                          description: Option[String],
                                          numberOfFilesUploaded: Int,
                                          uploadedFiles: Seq[FileTransferAudit]
                                        )

  object UpdateCaseAuditEventDetails {

    def from(
              updateRequest: AmendClaimRequest,
              updateResponse: NDRCCaseResponse
            ): JsValue = {
      val requestDetails: JsObject = Json
        .toJson(
          UpdateCaseAuditEventDetails(
            success = true,
            caseId = updateRequest.Content.CaseID,
            action = updateRequest.Content.selectedAmendments,
            description = Option(updateRequest.Content.Description),
            numberOfFilesUploaded = updateRequest.uploadedFiles.size,
            uploadedFiles = combineFileUploadAndTransferResults(
              updateRequest.uploadedFiles,
              updateResponse.result.map(_.fileTransferResults)
            )
          )
        )
        .as[JsObject]

      if (updateResponse.result.isDefined) requestDetails
      else
        (requestDetails ++ pegaResponseToDetails(updateResponse))
    }

    implicit val formats: Format[UpdateCaseAuditEventDetails] =
      Json.format[UpdateCaseAuditEventDetails]
  }

  def pegaResponseToDetails(
                             caseResponse: NDRCCaseResponse
                           ): JsObject =
    Json.obj(
      "success" -> caseResponse.isSuccess
    ) ++
      (if (caseResponse.isSuccess)
        Json.obj(
          "caseReferenceNumber" -> caseResponse.result.get.caseId
        )
      else Json.obj()) ++ caseResponse.error.map(e => Json.obj("errorCode" -> e.errorCode)).getOrElse(Json.obj()) ++
          caseResponse.error
            .flatMap(_.errorMessage)
            .map(m => Json.obj("errorMessage" -> m))
            .getOrElse(Json.obj())

  def combineFileUploadAndTransferResults(
                                           uploadedFiles: Seq[UploadedFile],
                                           fileTransferResults: Option[Seq[FileTransferResult]]
                                         ): Seq[FileTransferAudit] =
    uploadedFiles.map { upload =>
      val transferResultOpt = fileTransferResults.flatMap(_.find(_.upscanReference == upload.upscanReference))
      FileTransferAudit(
        upscanReference = upload.upscanReference,
        downloadUrl = upload.downloadUrl,
        uploadTimestamp = upload.uploadTimestamp,
        checksum = upload.checksum,
        fileName = upload.fileName,
        fileMimeType = upload.fileMimeType,
        transferSuccess = transferResultOpt.map(_.success).orElse(Some(false)),
        transferHttpStatus = transferResultOpt.map(_.httpStatus),
        transferredAt = transferResultOpt.map(_.transferredAt),
        transferError = transferResultOpt.flatMap(_.error)
      )
    }

}

