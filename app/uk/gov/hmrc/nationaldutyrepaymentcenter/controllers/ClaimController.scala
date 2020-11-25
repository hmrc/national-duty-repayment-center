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

package uk.gov.hmrc.nationaldutyrepaymentcenter.controllers

import java.{util => ju}

import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.nationaldutyrepaymentcenter.connectors.CreateCaseConnector
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.requests.{CreateClaimRequest, EISCreateCaseRequest, EISCreateCaseRequestContent}
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.responses.{ApiError, EISCreateCaseError, EISCreateCaseSuccess, NDRCCreateCaseResponse}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.ExecutionContext

@Singleton
abstract class ClaimController @Inject()(
                                 cc: ControllerComponents,
                                 createCaseConnector: CreateCaseConnector
                               )
                                        (implicit ec: ExecutionContext) extends BackendController(cc) with AuthActions with ControllerHelper {

  def submitClaim(): Action[String] = Action.async(parse.tolerantText) {
    implicit request =>
      withAuthorised {
        val correlationId = request.headers
          .get("x-correlation-id")
          .getOrElse(ju.UUID.randomUUID().toString())

        withPayload[CreateClaimRequest] { createCaseRequest =>
          val createClaimRequest = EISCreateCaseRequest(
            AcknowledgementReference = correlationId.replace("-", ""),
            ApplicationType = "NDRC",
            OriginatingSystem = "Digital",
            Content = EISCreateCaseRequestContent.from(createCaseRequest)
          )

          createCaseConnector.submitClaim(createClaimRequest, correlationId).map {
            case success: EISCreateCaseSuccess =>
              Created(
                Json.toJson(
                  NDRCCreateCaseResponse(
                    correlationId = correlationId,
                    result = Some(success.CaseID)
                  )
                )
              )
            // when request to the upstream api returns an error
            case error: EISCreateCaseError =>
              if (error.isDuplicateCaseError)
                Conflict(
                  Json.toJson(
                    NDRCCreateCaseResponse(
                      correlationId = correlationId,
                      error = Some(
                        ApiError(
                          errorCode = "409",
                          errorMessage = error.duplicateCaseID
                        )
                      )
                    )
                  )
                )
              else
                BadRequest(
                  Json.toJson(
                    NDRCCreateCaseResponse(
                      correlationId = correlationId,
                      error = Some(
                        ApiError(
                          errorCode = error.errorCode.getOrElse("ERROR_UPSTREAM_UNDEFINED"),
                          errorMessage = error.errorMessage
                        )
                      )
                    )
                  )
                )
          }

        } {
          // when incoming request's payload validation fails
          case (errorCode, errorMessage) =>
            BadRequest(
              Json.toJson(
                NDRCCreateCaseResponse(
                  correlationId = correlationId,
                  error = Some(
                    ApiError(errorCode, Some(errorMessage))
                  )
                )
              )
            )
        }
      }
  }
}