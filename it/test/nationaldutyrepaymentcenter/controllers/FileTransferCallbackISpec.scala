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

package nationaldutyrepaymentcenter.controllers

import java.time.Clock
import java.{util => ju}

import nationaldutyrepaymentcenter.stubs.{AmendCaseStubs, AuthStubs, DataStreamStubs, FileTransferStubs}
import nationaldutyrepaymentcenter.support.{JsonMatchers, ServerBaseISpec}
import org.mockito.Mockito.when
import org.scalatest.Suite
import org.scalatestplus.play.ServerProvider
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import uk.gov.hmrc.nationaldutyrepaymentcenter.services.UUIDGenerator

class FileTransferCallbackISpec
    extends ServerBaseISpec with AuthStubs with AmendCaseStubs with JsonMatchers with FileTransferStubs
    with DataStreamStubs {

  this: Suite with ServerProvider =>

  val url = s"http://localhost:$port"

  override def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.auth.port"                     -> wireMockPort,
        "microservice.services.eis.createcaseapi.host"        -> wireMockHost,
        "microservice.services.eis.createcaseapi.port"        -> wireMockPort,
        "microservice.services.eis.createcaseapi.token"       -> "dummy-it-token",
        "microservice.services.eis.createcaseapi.environment" -> "it",
        "metrics.enabled"                                     -> true,
        "auditing.enabled"                                    -> true,
        "auditing.consumer.baseUri.host"                      -> wireMockHost,
        "auditing.consumer.baseUri.port"                      -> wireMockPort,
        "microservice.services.file-transfer.host"            -> wireMockHost,
        "microservice.services.file-transfer.port"            -> wireMockPort
      ).overrides(bind[Clock].toInstance(clock), bind[UUIDGenerator].toInstance(uuidGeneratorMock))

  override lazy val app = appBuilder.build()
  val wsClient          = app.injector.instanceOf[WSClient]
  val uuidGenerator     = app.injector.instanceOf[UUIDGenerator]

  "FileTransferController" when {
    "POST /file-transfer-callback" should {
      "return 201 and audit the file transfer results when transfer successful" in {

        val correlationId = ju.UUID.randomUUID().toString()
        when(uuidGenerator.uuid).thenReturn(correlationId)

        val fileTransferResult = multiFileResponse(correlationId)

        givenAuditConnector()

        val result = wsClient
          .url(s"$url/file-transfer-callback")
          .withHttpHeaders("X-Correlation-ID" -> correlationId)
          .post(Json.toJson(fileTransferResult))
          .futureValue

        result.status mustBe 201

        verifyFilesTransferSucceededAudit(1)
      }

      "return 201 and audit the file transfer results when transfer failed" in {

        val correlationId = ju.UUID.randomUUID().toString()
        when(uuidGenerator.uuid).thenReturn(correlationId)

        val fileTransferResult = multiFileResponse(correlationId)

        val failedFileTransferResult = fileTransferResult.copy(results =
          fileTransferResult.results.map(r => r.copy(success = false, error = Some("SomeError")))
        )

        givenAuditConnector()

        val result = wsClient
          .url(s"$url/file-transfer-callback")
          .withHttpHeaders("X-Correlation-ID" -> correlationId)
          .post(Json.toJson(failedFileTransferResult))
          .futureValue

        result.status mustBe 201

        verifyFilesTransferFailedAudit(1, "SomeError")
      }

    }
  }
}
