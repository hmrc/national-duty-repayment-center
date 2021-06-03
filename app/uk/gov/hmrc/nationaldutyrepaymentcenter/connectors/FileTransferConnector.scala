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
import com.kenshoo.play.metrics.Metrics
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpPost, HttpResponse}
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.{FileTransferRequest, FileTransferResult}
import uk.gov.hmrc.nationaldutyrepaymentcenter.wiring.AppConfig

import java.time.{Clock, LocalDateTime}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FileTransferConnector @Inject()(
  val config: AppConfig,
  val http: HttpPost,
  val clock: Clock,
  metrics: Metrics
) extends HttpAPIMonitor {

  override val kenshooRegistry: MetricRegistry = metrics.defaultRegistry

  val url: String = config.fileBaseUrl + config.fileBasePath

  final def transferFile(fileTransferRequest: FileTransferRequest)(implicit
                                                                                          hc: HeaderCarrier,
                                                                                          ec: ExecutionContext
  ): Future[FileTransferResult] =
    monitor(s"ConsumedAPI-national-duty-repayment-center-file-api-POST") {
      http
        .POST[FileTransferRequest, HttpResponse](url, fileTransferRequest)
        .map(response =>
          FileTransferResult(
            fileTransferRequest.upscanReference,
            isSuccess(response),
            response.status,
            LocalDateTime.now(clock),
            None
          )
        )
    }

  private def isSuccess(response: HttpResponse): Boolean =
    response.status >= 200 && response.status < 300

}
