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

package nationaldutyrepaymentcenter.connectors

import nationaldutyrepaymentcenter.stubs.FileTransferStubs
import nationaldutyrepaymentcenter.support.AppBaseISpec
import play.api.Application
import uk.gov.hmrc.http._
import uk.gov.hmrc.nationaldutyrepaymentcenter.connectors.FileTransferConnector

import scala.concurrent.ExecutionContext.Implicits.global

class FileTransferConnectorISpec extends FileTransferConnectorISpecSetup with FileTransferStubs {

  "FileTransferConnector" when {
    "transferMultipleFiles" should {
      "return 202 if success" in {

        val request = multiFileRequest(conversationId)
        givenFileTransmissionsMultipleSucceeds(request, multiFileResponse(conversationId))
        val result = await(connector.transferMultipleFiles(request))
        result.status mustBe 202
      }
    }
  }
}

trait FileTransferConnectorISpecSetup extends AppBaseISpec with FileTransferStubs {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  override def fakeApplication(): Application = appBuilder.build()

  lazy val connector: FileTransferConnector =
    app.injector.instanceOf[FileTransferConnector]

}
