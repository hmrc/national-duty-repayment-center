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

package uk.gov.hmrc.nationaldutyrepaymentcenter.connectors

import com.codahale.metrics.MetricRegistry
import com.google.inject.Inject
import org.apache.pekko.actor.ActorSystem
import uk.gov.hmrc.play.bootstrap.metrics.Metrics
import play.api.Logger
import play.api.libs.json.Writes
import uk.gov.hmrc.http.{HeaderCarrier, _}
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.requests.EISAmendCaseRequest
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.responses._
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

  lazy private val logger = Logger(getClass)

  override val metricRegistry: MetricRegistry = metrics.defaultRegistry

  val url: String = config.eisBaseUrl + config.eisAmendCaseApiPath

  val serviceName: String = "ConsumedAPI-eis-pega-amend-case-api-POST"

  def submitAmendClaim(request: EISAmendCaseRequest, correlationId: String)(implicit
    hc: HeaderCarrier
  ): Future[EISAmendCaseResponse] =
    retry(config.retryDurations: _*)(
      EISAmendCaseResponse.shouldRetry,
      EISAmendCaseResponse.errorMessage,
      EISAmendCaseResponse.delayInterval
    ) {
      monitor(serviceName) {
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
    } recoverWith {
      case e: GatewayTimeoutException =>
        logger.error(s"$serviceName to $url failed with status: ${e.responseCode}")
        throw new GatewayTimeoutException(e.getMessage)
      case e =>
        logger.error(s"$serviceName to $url failed with unexpected response")
        throw new Exception(e.getMessage)
    }

}
