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

import java.time.format.DateTimeFormatter
import java.time.{ZoneId, ZonedDateTime}
import java.{util => ju}

import com.google.inject.Inject
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.http.{HeaderCarrier, _}
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.requests.EISCreateCaseRequest
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.responses.EISCreateCaseResponse
import uk.gov.hmrc.nationaldutyrepaymentcenter.wiring.AppConfig

import scala.concurrent.{ExecutionContext, Future}

class CreateCaseConnector @Inject()(
                                     val config: AppConfig,
                                     val http: HttpPost
                                   )(
                                     implicit ec: ExecutionContext
                                   ) extends HttpErrorFunctions {

  val httpDateFormat = DateTimeFormatter
    .ofPattern("EEE, dd MMM yyyy HH:mm:ss z", ju.Locale.ENGLISH)
    .withZone(ZoneId.of("GMT"))

  val url = config.eisBaseUrl + config.eisCreateCaseApiPath


  def submitClaim(request: EISCreateCaseRequest, correlationId: String)(implicit
                                                                        hc: HeaderCarrier,
                                                                        ec: ExecutionContext
  ): Future[EISCreateCaseResponse] = {
    http.POST[EISCreateCaseRequest, EISCreateCaseResponse](url, request)(
      implicitly,
      implicitly,
      HeaderCarrier(authorization = Some(Authorization(s"Bearer ${config.eisAuthorizationToken}")))
        .withExtraHeaders(
          "x-correlation-id" -> correlationId,
          "CustomProcessesHost" -> "Digital", // required by PEGA API spec
          "date" -> httpDateFormat.format(ZonedDateTime.now),
          "accept" -> "application/json",
          "environment" -> config.eisEnvironment
        ),
      implicitly[ExecutionContext]
    )
  }

}
