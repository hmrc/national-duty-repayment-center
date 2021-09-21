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

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json.JsValue
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.MultiFileTransferResult
import uk.gov.hmrc.nationaldutyrepaymentcenter.services.AuditService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FileTransferController @Inject() (val cc: ControllerComponents, auditService: AuditService)(implicit
  ec: ExecutionContext
) extends BackendController(cc) {

  lazy private val logger = Logger(getClass)

  def callback(): Action[JsValue] = Action(parse.json).async { implicit request =>
    withJsonBody[MultiFileTransferResult] { result =>
      val numberOfFailures: Int = result.results.count(file => !file.success)
      if (numberOfFailures > 0)
        logger.warn(
          s"MultiFileTransferResult contained failures, caseReferenceNumber:[${result.caseReferenceNumber}]  ${numberOfFailures}/${result.results.size} file(s) failed for ${result.conversationId}"
        )
      else
        logger.info(
          s"MultiFileTransferResult success, caseReferenceNumber:[${result.caseReferenceNumber}]  ${result.results.size} file(s) for ${result.conversationId}"
        )

      auditService.auditFileTransferResults(result)

      Future.successful(Created)
    }
  }

}
