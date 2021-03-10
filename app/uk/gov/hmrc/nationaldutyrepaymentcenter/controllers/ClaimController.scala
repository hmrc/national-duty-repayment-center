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

package uk.gov.hmrc.nationaldutyrepaymentcenter.controllers


import java.{util => ju}
import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.nationaldutyrepaymentcenter.connectors._
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.{FileTransferRequest, FileTransferResult, UploadedFile}
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.requests._
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.responses._
import uk.gov.hmrc.nationaldutyrepaymentcenter.services.{AuditService, ClaimService}
import uk.gov.hmrc.nationaldutyrepaymentcenter.wiring.AppConfig
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.LocalDateTime
import scala.concurrent.{ExecutionContext, Future}


class UUIDGenerator {
  def uuid: String = ju.UUID.randomUUID().toString()
}

@Singleton
class ClaimController @Inject()(
                                 val authConnector: MicroserviceAuthConnector,
                                 val fileTransferConnector: FileTransferConnector,
                                 val cc: ControllerComponents,
                                 val appConfig: AppConfig,
                                 val claimService: ClaimService,
                                 val uuidGenerator: UUIDGenerator,
                                 val auditService: AuditService,
                               )
                               (implicit ec: ExecutionContext) extends BackendController(cc) with AuthActions with ControllerHelper {

  def submitClaim(): Action[String] = Action(parse.tolerantText).async { implicit request =>
    val correlationId = request.headers
      .get("x-correlation-id")
      .getOrElse(uuidGenerator.uuid)

    withAuthorised {
      withPayload[CreateClaimRequest]{ createCaseRequest =>
        val eisCreateCaseRequest = EISCreateCaseRequest(
          AcknowledgementReference = correlationId.replace("-", ""),
          ApplicationType = "NDRC",
          OriginatingSystem = "Digital",
          Content = EISCreateCaseRequest.Content.from(createCaseRequest)
        )

        claimService.createClaim(eisCreateCaseRequest, correlationId).flatMap {
          case success: EISCreateCaseSuccess =>
            transferFilesToPega(success.CaseID, correlationId, createCaseRequest.uploadedFiles)
              .flatMap { fileTransferResults =>
                val response = NDRCCaseResponse(
                  correlationId = correlationId,
                  result = Option(
                    NDRCFileTransferResult(success.CaseID, LocalDateTime.now(), fileTransferResults)
                  ))
                auditService
                  .auditCreateCaseEvent(createCaseRequest)(response)
                  .map(_ => Created(Json.toJson(response)))
              }

          // when request to the upstream api returns an error
          case error: EISCreateCaseError => {
              val response = NDRCCaseResponse(
                correlationId = correlationId,
                error = Some(
                  ApiError(
                    errorCode = error.errorCode.getOrElse("ERROR_UPSTREAM_UNDEFINED"),
                    errorMessage = error.errorMessage
                  )
                )
              )
              auditService
                .auditCreateCaseEvent(createCaseRequest)(response)
                .map(_ => BadRequest(Json.toJson(response)))
            }
      }} {
        // when incoming request's payload validation fails
        case (errorCode, errorMessage) =>
          val response = NDRCCaseResponse(
            correlationId = correlationId,
            error = Some(
              ApiError(errorCode, Some(errorMessage))
            )
          )
          auditService
            .auditCreateCaseErrorEvent(response)
            .map(_ => BadRequest(Json.toJson(response)))
      }
    }.recoverWith {
      // last resort fallback when request processing fails
      case e =>
        val response = NDRCCaseResponse(
          correlationId = correlationId,
          error = Some(
            ApiError("500", Some(e.getMessage()))
          )
        )
        auditService
          .auditCreateCaseErrorEvent(response)
          .map(_ => InternalServerError(Json.toJson(response)))
    }
  }

  def submitAmendClaim(): Action[String] = Action(parse.tolerantText).async { implicit request =>

    val correlationId = request.headers
      .get("x-correlation-id")
      .getOrElse(uuidGenerator.uuid)

    withAuthorised {
      withPayload[AmendClaimRequest] { amendCaseRequest =>
        val eisAmendCaseRequest = EISAmendCaseRequest(
          AcknowledgementReference = correlationId.replace("-", ""),
          ApplicationType = "NDRC",
          OriginatingSystem = "Digital",
          Content = EISAmendCaseRequest.Content.from(amendCaseRequest)
        )
        claimService.amendClaim(eisAmendCaseRequest, correlationId).flatMap {
          case success: EISAmendCaseSuccess =>
            transferFilesToPega(success.CaseID, correlationId, amendCaseRequest.uploadedFiles)
              .flatMap { fileTransferResults =>
                val response = NDRCCaseResponse(
                  correlationId = correlationId,
                  result = Option(
                    NDRCFileTransferResult(success.CaseID, LocalDateTime.now(), fileTransferResults)
                  ))
                auditService.auditUpdateCaseEvent(amendCaseRequest)(response).map ( _ =>
                Created(Json.toJson(response)))
              }
          // when request to the upstream api returns an error
          case error: EISAmendCaseError =>
            val response = NDRCCaseResponse(
              correlationId = correlationId,
              error = Some(
                ApiError(
                  errorCode = error.errorCode.getOrElse("ERROR_UPSTREAM_UNDEFINED"),
                  errorMessage = error.errorMessage
                )
              )
            )
            auditService.auditUpdateCaseEvent(amendCaseRequest)(response)
              .map(_ => BadRequest(Json.toJson(response)))
        }
      } {
        // when incoming request's payload validation fails
        case (errorCode, errorMessage) =>
          val response = NDRCCaseResponse(
            correlationId = correlationId,
            error = Some(
              ApiError(errorCode, Some(errorMessage))
            )
          )
          auditService
            .auditUpdateCaseErrorEvent(response)
            .map(_ => BadRequest(Json.toJson(response)))
      }
    } .recoverWith {
      // last resort fallback when request processing fails
      case e =>
        val response = NDRCCaseResponse(
          correlationId = correlationId,
          error = Some(
            ApiError("500", Some(e.getMessage()))
          )
        )
        auditService
          .auditUpdateCaseErrorEvent(response)
          .map(_ => InternalServerError(Json.toJson(response)))
    }
  }
  def transferFilesToPega(
                           caseReferenceNumber: String,
                           conversationId: String,
                           uploadedFiles: Seq[UploadedFile]
                         )(implicit hc: HeaderCarrier): Future[Seq[FileTransferResult]] = {
    Future.sequence(
      uploadedFiles.zipWithIndex
        .map {
          case (file, index) =>
            FileTransferRequest
              .fromUploadedFile(
                caseReferenceNumber,
                conversationId,
                correlationId = uuidGenerator.uuid,
                applicationName = "NDRC",
                batchSize = uploadedFiles.size,
                batchCount = index + 1,
                uploadedFile = file
              )
        }
        .map(fileTransferConnector.transferFile(_, conversationId))
    )
  }
}
