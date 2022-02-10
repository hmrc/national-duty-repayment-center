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

package uk.gov.hmrc.nationaldutyrepaymentcenter.models

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import play.api.libs.json._

import scala.util.{Failure, Success, Try}

object JsonFormatUtils {

  def stringFormat[A](fromString: String => A)(makeString: A => String): Format[A] = new Format[A] {

    def reads(json: JsValue): JsResult[A] = json match {
      case JsString(str) => JsSuccess(fromString(str))
      case _             => JsError(s"Expected JSON string type")
    }

    def writes(o: A): JsValue = JsString(makeString(o))
  }

  val dateFormat: Format[LocalDate] = new Format[LocalDate] {
    val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

    override def writes(o: LocalDate): JsValue = JsString(o.format(formatter))

    override def reads(json: JsValue): JsResult[LocalDate] = json match {
      case JsString(s) ⇒
        Try(LocalDate.parse(s, formatter)) match {
          case Success(date)  ⇒ JsSuccess(date)
          case Failure(error) ⇒ JsError(s"Could not parse date as yyyyMMdd: ${error.getMessage}")
        }

      case other ⇒ JsError(s"Expected string but got $other")
    }

  }

}
