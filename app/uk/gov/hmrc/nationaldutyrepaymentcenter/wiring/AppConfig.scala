/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.nationaldutyrepaymentcenter.wiring

import java.util.concurrent.TimeUnit

import com.google.inject.ImplementedBy
import javax.inject.Inject
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.duration.FiniteDuration

@ImplementedBy(classOf[AppConfigImpl])
trait AppConfig {

  val appName: String

  val authBaseUrl: String

  val eisBaseUrl: String

  val eisCreateCaseApiPath: String

  val eisAmendCaseApiPath: String

  val eisAuthorizationToken: String

  val eisEnvironment: String

  val fileBaseUrl: String

  val fileBasePath: String

  val internalBaseUrl: String

  val retryDurations: Seq[FiniteDuration]

}

class AppConfigImpl @Inject() (config: ServicesConfig) extends AppConfig {

  override val appName: String = config.getString("appName")

  override val authBaseUrl: String = config.baseUrl("auth")

  override val eisBaseUrl: String = config.baseUrl("eis.createcaseapi")

  override val fileBaseUrl: String = config.baseUrl("file-transfer")

  override lazy val fileBasePath: String =
    config.getString("microservice.services.file-transfer.path-multiple")

  override val eisCreateCaseApiPath: String =
    config.getString("microservice.services.eis.createcaseapi.path")

  override val eisAmendCaseApiPath: String =
    config.getString("microservice.services.eis.amendcaseapi.path")

  override val eisAuthorizationToken: String =
    config.getString("microservice.services.eis.createcaseapi.token")

  override val eisEnvironment: String =
    config.getString("microservice.services.eis.createcaseapi.environment")

  override val internalBaseUrl: String =
    config.getString("urls.callback.internal")

  val retryDurations: Seq[FiniteDuration] =
    config.getString("retry.duration.seconds").split(",").map(secs =>
      FiniteDuration(secs.trim.toInt, TimeUnit.SECONDS)
    ).toIndexedSeq

}
