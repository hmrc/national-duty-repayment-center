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

package utils

import java.time._

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.inject.guice.GuiceApplicationBuilder

class UnitSpecBase extends AnyWordSpec with Matchers with ScalaFutures {

  protected def applicationBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()

  val fixedInstant: Instant     = LocalDateTime.parse("2027-11-02T16:33:51.880").toInstant(ZoneOffset.UTC)
  implicit val stubClock: Clock = Clock.fixed(fixedInstant, ZoneId.systemDefault)

}
