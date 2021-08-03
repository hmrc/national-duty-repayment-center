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

import java.time.LocalDateTime
import java.util.UUID

import javax.inject.Inject
import play.api.Logger
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.nationaldutyrepaymentcenter.connectors.FileTransferConnector
import uk.gov.hmrc.nationaldutyrepaymentcenter.controllers.routes
import uk.gov.hmrc.nationaldutyrepaymentcenter.models._
import uk.gov.hmrc.nationaldutyrepaymentcenter.wiring.AppConfig

import scala.concurrent.{ExecutionContext, Future}

class FileTransferService @Inject() (
  fileTransferConnector: FileTransferConnector,
  appConfig: AppConfig,
  auditService: AuditService
) {

  lazy private val logger = Logger(getClass)

  def transferMultipleFiles(
    caseReferenceNumber: String,
    conversationId: String,
    uploadedFiles: Seq[UploadedFile]
  )(implicit hc: HeaderCarrier, executionContext: ExecutionContext): Future[Unit] = {
    val request = MultiFileTransferRequest(
      conversationId,
      caseReferenceNumber,
      "NDRC",
      uploadedFiles.map(FileTransferData.fromUploadedFile),
      Some(appConfig.internalBaseUrl + routes.FileTransferController.callback().url)
    )

    fileTransferConnector.transferMultipleFiles(request)
      .recover {
        case error =>
          HttpResponse.apply(500, error.getMessage)
      }
      .map { result =>
        if (result.status != 202) {
          val errorId      = UUID.randomUUID().toString
          val errorMessage = s"TransferMultipleFiles failed [${result.status}] ${result.body}"
          logger.error(s"$errorMessage [$errorId]")
          auditService.auditFileTransferResults(buildErrorResult(request, errorMessage, errorId))
        }
      }
  }

  private def buildErrorResult(
    request: MultiFileTransferRequest,
    errorMessage: String,
    errorId: String
  ): MultiFileTransferResult = {
    val timeStamp = LocalDateTime.now()

    MultiFileTransferResult(
      request.conversationId,
      request.caseReferenceNumber,
      request.applicationName,
      0,
      request.files.map(
        file =>
          FileTransferResult(
            file.upscanReference,
            file.checksum,
            file.fileName,
            file.fileMimeType,
            success = false,
            0,
            timeStamp,
            errorId,
            0,
            Some(errorMessage)
          )
      )
    )
  }

}
