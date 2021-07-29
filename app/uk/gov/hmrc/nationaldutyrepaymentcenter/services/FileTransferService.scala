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

import akka.actor.{ActorRef, ActorSystem, Props}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.nationaldutyrepaymentcenter.actors.{FileTransferActor, FileTransferAuditActor}
import uk.gov.hmrc.nationaldutyrepaymentcenter.connectors.FileTransferConnector
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.{FileTransferResult, UploadedFile}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FileTransferService @Inject() (
  fileTransferConnector: FileTransferConnector,
  uuidGenerator: UUIDGenerator,
  auditConnector: AuditConnector,
  actorSystem: ActorSystem
) {

  def transferFiles(caseReferenceNumber: String, conversationId: String, uploadedFiles: Seq[UploadedFile])(implicit
    hc: HeaderCarrier,
    executionContext: ExecutionContext
  ): Future[Seq[FileTransferResult]] = {

    val auditActor: ActorRef = actorSystem.actorOf(
      Props(classOf[FileTransferAuditActor], caseReferenceNumber, auditConnector, hc, executionContext)
    )

    // Single-use actor responsible for transferring files batch to PEGA
    val fileTransferActor: ActorRef =
      actorSystem.actorOf(
        Props(
          classOf[FileTransferActor],
          caseReferenceNumber,
          fileTransferConnector,
          uuidGenerator,
          conversationId,
          auditActor
        )
      )

    fileTransferActor ! FileTransferActor.TransferMultipleFiles(uploadedFiles.zipWithIndex, uploadedFiles.size, hc)
    Future.successful(Seq.empty)
  }

}
