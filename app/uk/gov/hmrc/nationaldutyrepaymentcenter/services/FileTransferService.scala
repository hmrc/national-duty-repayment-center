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

import play.api.Logging
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.nationaldutyrepaymentcenter.connectors.FileTransferConnector
import uk.gov.hmrc.nationaldutyrepaymentcenter.controllers.routes
import uk.gov.hmrc.nationaldutyrepaymentcenter.models._
import uk.gov.hmrc.nationaldutyrepaymentcenter.wiring.AppConfig

import java.time.LocalDateTime
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FileTransferService @Inject() (
  fileTransferConnector: FileTransferConnector,
  appConfig: AppConfig,
  auditService: AuditService
) extends Logging {

  def transferMultipleFiles(
    caseReferenceNumber: String,
    conversationId: String,
    uploadedFiles: Seq[UploadedFile]
  )(implicit hc: HeaderCarrier, executionContext: ExecutionContext): Future[Unit] = uploadedFiles match {
    case files if files.nonEmpty =>
      val request = MultiFileTransferRequest(
        conversationId,
        caseReferenceNumber,
        "NDRC",
        files.map(FileTransferData.fromUploadedFile),
        Some(appConfig.internalBaseUrl + routes.FileTransferController.callback().url)
      )

      fileTransferConnector.transferMultipleFiles(request)
        .recover {
          case error =>
            HttpResponse.apply(500, error.getMessage)
        }
        .map { result =>
          if (result.status != 202) {
            val errorMessage =
              s"TransferMultipleFiles failed caseReferenceNumber:[${caseReferenceNumber}] for ${conversationId} [${result.status}] ${result.body} "
            logger.warn(s"$errorMessage [$conversationId]")
            auditService.auditFileTransferResults(buildErrorResult(request, errorMessage, conversationId))
          }
        }
    case _ => Future.successful(())
  }

  private def buildErrorResult(
    request: MultiFileTransferRequest,
    errorMessage: String,
    conversationId: String
  ): MultiFileTransferResult = {
    val timeStamp = LocalDateTime.now()

    MultiFileTransferResult(
      request.conversationId,
      request.caseReferenceNumber,
      request.applicationName,
      0,
      request.files.map(file =>
        FileTransferResult(
          file.upscanReference,
          file.checksum,
          file.fileName,
          file.fileMimeType,
          success = false,
          0,
          timeStamp,
          conversationId,
          0,
          Some(errorMessage)
        )
      )
    )
  }

}
