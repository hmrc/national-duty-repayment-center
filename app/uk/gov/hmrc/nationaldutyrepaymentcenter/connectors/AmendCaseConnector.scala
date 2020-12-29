/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.nationaldutyrepaymentcenter.connectors

import java.time.ZonedDateTime

import com.codahale.metrics.MetricRegistry
import com.google.inject.Inject
import com.kenshoo.play.metrics.Metrics
import play.api.libs.json.Writes
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.http.{HeaderCarrier, _}
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.requests.EISAmendCaseRequest
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.responses.{EISCreateCaseError, EISCreateCaseResponse, EISCreateCaseSuccess}
import uk.gov.hmrc.nationaldutyrepaymentcenter.wiring.AppConfig

import scala.concurrent.{ExecutionContext, Future}

class AmendCaseConnector @Inject()(
                                     val config: AppConfig,
                                     val http: HttpPost,
                                     metrics: Metrics
                                   )(
                                     implicit ec: ExecutionContext
                                   ) extends ReadSuccessOrFailure[EISCreateCaseResponse, EISCreateCaseSuccess, EISCreateCaseError](
  EISCreateCaseError.fromStatusAndMessage
) with PegaConnector with HttpAPIMonitor {

  override val kenshooRegistry: MetricRegistry = metrics.defaultRegistry

  val url = config.eisBaseUrl + config.eisAmendCaseApiPath

  def submitAmendClaim(request: EISAmendCaseRequest, correlationId: String)(implicit
                                                                        hc: HeaderCarrier,
                                                                        ec: ExecutionContext
  ): Future[EISCreateCaseResponse] = {
    http.POST[EISAmendCaseRequest, EISCreateCaseResponse](url, request)(
      implicitly[Writes[EISAmendCaseRequest]],
      readFromJsonSuccessOrFailure,
      HeaderCarrier(
        authorization = Some(Authorization(s"Bearer ${config.eisAuthorizationToken}"))
      )
        .withExtraHeaders(
          "x-correlation-id" -> correlationId,
          "CustomProcessesHost" -> "Digital",
          "date" -> httpDateFormat.format(ZonedDateTime.now),
          "accept" -> "application/json",
          "environment" -> config.eisEnvironment
        ),
      implicitly[ExecutionContext]
    )
  }
}
