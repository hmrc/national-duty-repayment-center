/*
 * Copyright 2023 HM Revenue & Customs
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

import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames}

import java.time.{ZoneId, ZonedDateTime}
import java.time.format.DateTimeFormatter
import java.{util => ju}

/** Provides EIS API headers */
trait EISConnector {

  final val httpDateFormat = DateTimeFormatter
    .ofPattern("EEE, dd MMM yyyy HH:mm:ss z", ju.Locale.ENGLISH)
    .withZone(ZoneId.of("GMT"))

  final def mdtpTracingHeaders(hc: HeaderCarrier): Seq[(String, String)] =
    Seq(
      hc.requestId.map(HeaderNames.xRequestId -> _.value),
      hc.sessionId.map(HeaderNames.xSessionId -> _.value)
    ).flatten

  /** Headers required by the EIS API */
  final def eisApiHeaders(correlationId: String, environment: String, token: String): Seq[(String, String)] =
    Seq(
      "x-correlation-id"    -> correlationId,
      "CustomProcessesHost" -> "Digital",
      "date"                -> httpDateFormat.format(ZonedDateTime.now),
      "accept"              -> "application/json",
      "environment"         -> environment,
      "Authorization"       -> s"Bearer $token"
    )

}
