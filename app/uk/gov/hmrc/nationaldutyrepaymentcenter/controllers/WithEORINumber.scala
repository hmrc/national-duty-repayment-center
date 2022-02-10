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

package uk.gov.hmrc.nationaldutyrepaymentcenter.controllers

import play.api.mvc.{Result, _}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.allEnrolments
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.EORI

import scala.concurrent.{ExecutionContext, Future}

trait WithEORINumber extends AuthorisedFunctions { self: Results =>

  private val eoriEnrolment  = "HMRC-CTS-ORG"
  private val eoriIdentifier = "EORINumber"

  protected def withEORINumber(
    f: Option[EORI] => Future[Result]
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Result] =
    authorised().retrieve(allEnrolments) { enrolments =>
      val eoriFromEnrolments =
        enrolments
          .enrolments.find(en => eoriEnrolment == en.key)
          .flatMap(_.getIdentifier(eoriIdentifier)).map(e => EORI(e.value))

      f(eoriFromEnrolments)
    }

}
