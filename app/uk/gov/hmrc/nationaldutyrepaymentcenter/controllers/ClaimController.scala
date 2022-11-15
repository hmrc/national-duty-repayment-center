/*
 * Copyright 2022 HM Revenue & Customs
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

import play.api.Logger

import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.http.TooManyRequestException
import uk.gov.hmrc.nationaldutyrepaymentcenter.connectors._
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.requests._
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.responses._
import uk.gov.hmrc.nationaldutyrepaymentcenter.services.{AuditService, ClaimService, FileTransferService, UUIDGenerator}
import uk.gov.hmrc.nationaldutyrepaymentcenter.wiring.AppConfig
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.ExecutionContext

@Singleton
class ClaimController @Inject() (
  val authConnector: MicroserviceAuthConnector,
  fileTransferService: FileTransferService,
  val uuidGenerator: UUIDGenerator,
  val cc: ControllerComponents,
  val appConfig: AppConfig,
  val claimService: ClaimService,
  val auditService: AuditService
)(implicit ec: ExecutionContext)
    extends BackendController(cc) with AuthActions with WithCorrelationId with WithEORINumber {

  lazy private val logger = Logger(getClass)

  private def acknowledgementReferenceFrom(correlationId: String): String =
    correlationId.replace("-", "").takeRight(32)

  def submitClaim(): Action[JsValue] = Action(parse.json).async { implicit request =>
    withCorrelationId { correlationId: String =>
      withAuthorised {
        withEORINumber { maybeEORI =>
          withJsonBody[CreateClaimRequest] { createCaseRequest =>
            val eisCreateCaseRequest = EISCreateCaseRequest(
              AcknowledgementReference = acknowledgementReferenceFrom(correlationId),
              ApplicationType = "NDRC",
              OriginatingSystem = "Digital",
              Content = EISCreateCaseRequest.Content.from(createCaseRequest)
            )
            claimService.createClaim(eisCreateCaseRequest, correlationId).flatMap {
              case success: EISCreateCaseSuccess =>
                fileTransferService.transferMultipleFiles(
                  success.CaseID,
                  correlationId,
                  createCaseRequest.uploadedFiles
                )
                  .flatMap { _ =>
                    val response = NDRCCaseResponse(caseId = Some(success.CaseID), correlationId = correlationId)
                    logger.info(
                      s"CreateCaseEvent success, caseReferenceNumber:[${success.CaseID}] correlationId:[$correlationId]"
                    )
                    auditService.auditCreateCaseEvent(createCaseRequest, response, maybeEORI)
                      .map(_ => Created(Json.toJson(response)))
                  }

              // when request to the upstream api returns an error
              case error: EISCreateCaseError =>
                val response = NDRCCaseResponse(
                  correlationId = correlationId,
                  caseId = None,
                  error = Some(
                    ApiError(
                      errorCode = error.errorCode.getOrElse("ERROR_UPSTREAM_UNDEFINED"),
                      errorMessage = error.errorMessage
                    )
                  )
                )
                logger.info(
                  s"CreateCaseEvent failed with bad request, caseReferenceNumber:[] correlationId:[$correlationId]"
                )
                auditService
                  .auditCreateCaseEvent(createCaseRequest, response, maybeEORI)
                  .map(_ => BadRequest(Json.toJson(response)))
            }
          }
        }.recoverWith {
          // last resort fallback when request processing fails
          case e =>
            val response = responseForError(e, correlationId)
            logger.error(
              s"CreateCaseEvent failed with internal server error, caseReferenceNumber:[] correlationId:[$correlationId]"
            )
            auditService
              .auditCreateCaseErrorEvent(response)
              .map(_ => InternalServerError(Json.toJson(response)))
        }
      }
    }
  }

  def submitAmendClaim(): Action[JsValue] = Action(parse.json).async { implicit request =>
    withCorrelationId { correlationId: String =>
      withAuthorised {
        withEORINumber { maybeEORI =>
          withJsonBody[AmendClaimRequest] { amendCaseRequest =>
            val eisAmendCaseRequest = EISAmendCaseRequest(
              AcknowledgementReference = acknowledgementReferenceFrom(correlationId),
              ApplicationType = "NDRC",
              OriginatingSystem = "Digital",
              EISAmendCaseRequest.Content.from(amendCaseRequest)
            )
            claimService.amendClaim(eisAmendCaseRequest, correlationId).flatMap {
              case success: EISAmendCaseSuccess =>
                fileTransferService.transferMultipleFiles(success.CaseID, correlationId, amendCaseRequest.uploadedFiles)
                  .flatMap { _ =>
                    val response = NDRCCaseResponse(correlationId = correlationId, caseId = Some(success.CaseID))
                    logger.info(
                      s"UpdateCaseEvent success, caseReferenceNumber:[${eisAmendCaseRequest.Content.CaseID}] correlationId:[$correlationId]"
                    )
                    auditService.auditUpdateCaseEvent(amendCaseRequest, response, maybeEORI).map(_ =>
                      Created(Json.toJson(response))
                    )
                  }
              // when request to the upstream api returns an error
              case error: EISAmendCaseError =>
                val response = NDRCCaseResponse(
                  correlationId = correlationId,
                  caseId = None,
                  error = Some(
                    ApiError(
                      errorCode = error.errorCode.getOrElse("ERROR_UPSTREAM_UNDEFINED"),
                      errorMessage = error.errorMessage
                    )
                  )
                )
                logger.info(
                  s"UpdateCaseEvent failed with bad request, caseReferenceNumber:[${eisAmendCaseRequest.Content.CaseID}] correlationId:[$correlationId]"
                )
                auditService.auditUpdateCaseEvent(amendCaseRequest, response, maybeEORI)
                  .map(_ => BadRequest(Json.toJson(response)))
            }
          }
        }.recoverWith {
          // last resort fallback when request processing fails
          case e =>
            val response = responseForError(e, correlationId)
            logger.error(
              s"UpdateCaseEvent failed with internal server error, caseReferenceNumber:[] correlationId:[$correlationId]"
            )
            auditService
              .auditUpdateCaseErrorEvent(response)
              .map(_ => InternalServerError(Json.toJson(response)))
        }
      }
    }
  }

  private def responseForError(e: Throwable, correlationId: String): NDRCCaseResponse = {
    val message =
      if (e.isInstanceOf[TooManyRequestException])
        s"Failed after ${appConfig.retryDurations.size} retries"
      else e.getMessage

    NDRCCaseResponse(correlationId = correlationId, caseId = None, error = Some(ApiError("500", Some(message))))
  }

}
