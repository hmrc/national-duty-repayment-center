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

package uk.gov.hmrc.nationaldutyrepaymentcenter.connectors

import com.codahale.metrics.MetricRegistry
import com.google.inject.Inject
import com.kenshoo.play.metrics.Metrics
import play.api.libs.json.Writes
import uk.gov.hmrc.http.{HeaderCarrier, _}
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.requests.EISCreateCaseRequest
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.responses.{EISCreateCaseError, EISCreateCaseResponse, EISCreateCaseSuccess}
import uk.gov.hmrc.nationaldutyrepaymentcenter.wiring.AppConfig

import scala.concurrent.{ExecutionContext, Future}

class CreateCaseConnector @Inject()(
                                     val config: AppConfig,
                                     val http: HttpPost,
                                     metrics: Metrics
                                   )(
                                     implicit ec: ExecutionContext
                                   ) extends ReadSuccessOrFailure[EISCreateCaseResponse, EISCreateCaseSuccess, EISCreateCaseError](
  EISCreateCaseError.fromStatusAndMessage
) with EISConnector with HttpAPIMonitor {

  override val kenshooRegistry: MetricRegistry = metrics.defaultRegistry

  val url: String = config.eisBaseUrl + config.eisCreateCaseApiPath


  def submitClaim(request: EISCreateCaseRequest, correlationId: String)(implicit hc: HeaderCarrier
  ): Future[EISCreateCaseResponse] = {
    monitor(s"ConsumedAPI-eis-pega-create-case-api-POST") {
      http.POST[EISCreateCaseRequest, EISCreateCaseResponse](
        url,
        request,
        eisApiHeaders(correlationId, config.eisEnvironment, config.eisAuthorizationToken) ++ mdtpTracingHeaders(hc)
      )(
        implicitly[Writes[EISCreateCaseRequest]],
        readFromJsonSuccessOrFailure,
        hc.copy(
          authorization = None, // sent via EISApiHeaders
          requestId = None, // sent via MDTPTracingHeaders (None here prevents sending duplicate header to internal stubs)
          sessionId = None // sent via MDTPTracingHeaders (None here prevents sending duplicate header to internal stubs)
        ),
        implicitly[ExecutionContext]
      )
    }
  }
}
