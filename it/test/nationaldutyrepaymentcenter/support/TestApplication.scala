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

package nationaldutyrepaymentcenter.support

import java.time.{Clock, Instant, ZoneId}

import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.nationaldutyrepaymentcenter.services.UUIDGenerator

trait TestApplication {
  _: BaseISpec =>

  override implicit lazy val app: Application = appBuilder.build()
  val uuidGeneratorMock: UUIDGenerator        = mock[UUIDGenerator]
  val clock: Clock                            = Clock.fixed(Instant.parse("2020-09-09T10:15:30.00Z"), ZoneId.of("UTC"))

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.auth.port"                     -> wireMockPort,
        "microservice.services.eis.createcaseapi.host"        -> wireMockHost,
        "microservice.services.eis.createcaseapi.port"        -> wireMockPort,
        "microservice.services.eis.createcaseapi.token"       -> "dummy-it-token",
        "microservice.services.eis.createcaseapi.environment" -> "it",
        "metrics.enabled"                                     -> true,
        "auditing.enabled"                                    -> false,
        "auditing.consumer.baseUri.host"                      -> wireMockHost,
        "auditing.consumer.baseUri.port"                      -> wireMockPort,
        "microservice.services.file-transfer.host"            -> wireMockHost,
        "microservice.services.file-transfer.port"            -> wireMockPort
      ).overrides(
        bind[Clock].toInstance(clock),
        bind[UUIDGenerator].toInstance(uuidGeneratorMock)
      )

}
