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

package uk.gov.hmrc.nationaldutyrepaymentcenter.actors

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.mockito.MockitoSugar.mock
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}
import uk.gov.hmrc.nationaldutyrepaymentcenter.connectors.FileTransferConnector
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.{FileTransferResult, UploadedFile}
import uk.gov.hmrc.nationaldutyrepaymentcenter.services.UUIDGenerator
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import java.time.{LocalDateTime, ZonedDateTime}
import scala.concurrent.{ExecutionContext, Future}

class FileTransferActorSpec
    extends TestKit(ActorSystem("FileTransferActorSpec")) with ImplicitSender with AnyWordSpecLike
    with BeforeAndAfterAll with BeforeAndAfterEach {

  private val mockFileTransferConnector = mock[FileTransferConnector]
  private val mockAuditConnector        = mock[AuditConnector]
  private val mockUuidGenerator         = mock[UUIDGenerator]
  private val mockHeaderCarrier         = mock[HeaderCarrier]
  private val mockExceutionContext      = mock[ExecutionContext]

  private val testAuditActorRef = TestActorRef(
    new FileTransferAuditActor("my-case-ref", mockAuditConnector, mockHeaderCarrier, mockExceutionContext)
  )

  override protected def beforeEach(): Unit = {

    val transferInstant = LocalDateTime.now()
    when(mockFileTransferConnector.transferFile(any())(any(), any()))
      .thenReturn(Future.successful(FileTransferResult("ups-123", true, 202, transferInstant)))
  }

  override def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  "File transfer actor" must {
    "transfer files" in {

      val transferrer = system.actorOf(
        Props(
          classOf[FileTransferActor],
          "my-case-ref",
          mockFileTransferConnector,
          mockUuidGenerator,
          "conv-id",
          testAuditActorRef
        )
      )
      val files = Seq(
        UploadedFile(
          "ups-123",
          "/upscan/ups-123",
          ZonedDateTime.now(),
          "valid",
          "important-form.pdf",
          "application/pdf"
        )
      )

      transferrer ! FileTransferActor.TransferMultipleFiles(files.zipWithIndex, files.size, mockHeaderCarrier)

    }

    "handle failed transfers" in {

      when(mockFileTransferConnector.transferFile(any())(any(), any()))
        .thenReturn(Future.failed(new Exception("unexpected")))

      val transferrer = system.actorOf(
        Props(
          classOf[FileTransferActor],
          "my-case-ref",
          mockFileTransferConnector,
          mockUuidGenerator,
          "conv-id",
          testAuditActorRef
        )
      )
      val files = Seq(
        UploadedFile(
          "ups-123",
          "/upscan/ups-123",
          ZonedDateTime.now(),
          "valid",
          "important-form.pdf",
          "application/pdf"
        )
      )

      transferrer ! FileTransferActor.TransferMultipleFiles(files.zipWithIndex, files.size, mockHeaderCarrier)

    }

    "handle upstream errors" in {

      when(mockFileTransferConnector.transferFile(any())(any(), any()))
        .thenReturn(Future.failed(new NotFoundException("File not found")))

      val transferrer = system.actorOf(
        Props(
          classOf[FileTransferActor],
          "my-case-ref",
          mockFileTransferConnector,
          mockUuidGenerator,
          "conv-id",
          testAuditActorRef
        )
      )
      val files = Seq(
        UploadedFile(
          "ups-123",
          "/upscan/ups-123",
          ZonedDateTime.now(),
          "valid",
          "important-form.pdf",
          "application/pdf"
        )
      )

      transferrer ! FileTransferActor.TransferMultipleFiles(files.zipWithIndex, files.size, mockHeaderCarrier)

    }
  }

}
