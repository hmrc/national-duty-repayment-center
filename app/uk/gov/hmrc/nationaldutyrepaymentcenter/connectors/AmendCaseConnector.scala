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

package uk.gov.hmrc.nationaldutyrepaymentcenter.connectors

import akka.actor.ActorSystem
import com.codahale.metrics.MetricRegistry
import com.google.inject.Inject
import com.kenshoo.play.metrics.Metrics
import play.api.libs.json.Writes
import uk.gov.hmrc.http.{HeaderCarrier, _}
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.requests.EISAmendCaseRequest
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.responses.{
  EISAmendCaseError,
  EISAmendCaseResponse,
  EISAmendCaseSuccess
}
import uk.gov.hmrc.nationaldutyrepaymentcenter.wiring.AppConfig

import scala.concurrent.{ExecutionContext, Future}

class AmendCaseConnector @Inject() (
  val config: AppConfig,
  val http: HttpPost,
  val actorSystem: ActorSystem,
  metrics: Metrics
)(implicit ec: ExecutionContext)
    extends ReadSuccessOrFailure[EISAmendCaseResponse, EISAmendCaseSuccess, EISAmendCaseError](
      EISAmendCaseError.fromStatusAndMessage
    ) with EISConnector with HttpAPIMonitor with Retry {

  override val kenshooRegistry: MetricRegistry = metrics.defaultRegistry

  val url: String = config.eisBaseUrl + config.eisAmendCaseApiPath

  def submitAmendClaim(request: EISAmendCaseRequest, correlationId: String)(implicit
    hc: HeaderCarrier
  ): Future[EISAmendCaseResponse] =
    retry(config.retryDurations: _*)(
      EISAmendCaseResponse.shouldRetry,
      EISAmendCaseResponse.errorMessage,
      EISAmendCaseResponse.delayInterval
    ) {
      monitor(s"ConsumedAPI-eis-pega-amend-case-api-POST") {
        http.POST[EISAmendCaseRequest, EISAmendCaseResponse](
          url,
          request,
          eisApiHeaders(correlationId, config.eisEnvironment, config.eisAuthorizationToken) ++ mdtpTracingHeaders(hc)
        )(
          implicitly[Writes[EISAmendCaseRequest]],
          readFromJsonSuccessOrFailure,
          hc.copy(authorization = None),
          implicitly[ExecutionContext]
        )
      }
    }

}
