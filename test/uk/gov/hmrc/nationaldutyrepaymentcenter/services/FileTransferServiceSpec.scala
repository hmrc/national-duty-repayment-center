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

import java.time.ZonedDateTime

import base.SpecBase
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, verifyNoInteractions, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.nationaldutyrepaymentcenter.connectors.FileTransferConnector
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.UploadedFile
import uk.gov.hmrc.nationaldutyrepaymentcenter.wiring.AppConfig

import scala.concurrent.{ExecutionContext, Future}

class FileTransferServiceSpec extends SpecBase {

  implicit val executionContext: ExecutionContext =
    scala.concurrent.ExecutionContext.Implicits.global

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val mockConnector: FileTransferConnector = mock[FileTransferConnector]
  val appConfig: AppConfig                 = injector.instanceOf[AppConfig]
  val auditService: AuditService           = injector.instanceOf[AuditService]

  val service = new FileTransferService(mockConnector, appConfig, auditService)

  "FileTransferService" should {

    "not call file transfer if empty list provided" in {

      service.transferMultipleFiles("caseRef", "conversationId", Seq.empty).futureValue

      verifyNoInteractions(mockConnector)
    }

    "call connector to transfer files" in {

      when(mockConnector.transferMultipleFiles(any())(any(), any())) thenReturn Future.successful(
        HttpResponse.apply(202, "")
      )

      val files = Seq(
        UploadedFile(
          "ref-123",
          downloadUrl = "/bucket/test1.jpeg",
          uploadTimestamp = ZonedDateTime.now(),
          checksum = "checksum",
          fileName = "test1.jpeg",
          fileMimeType = "image/jpeg"
        )
      )

      service.transferMultipleFiles("caseRef", "conversationId", files).futureValue

      verify(mockConnector).transferMultipleFiles(any())(any(), any())
    }
  }

}
