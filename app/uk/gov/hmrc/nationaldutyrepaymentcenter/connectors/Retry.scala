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

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.pattern.after
import play.api.Logger
import uk.gov.hmrc.play.http.logging.Mdc

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

trait Retry {

  protected def actorSystem: ActorSystem

  def retry[A](
    intervals: FiniteDuration*
  )(shouldRetry: Try[A] => Boolean, retryReason: Try[A] => String, delayInterval: Try[A] => Option[FiniteDuration])(
    block: => Future[A]
  )(implicit ec: ExecutionContext): Future[A] = {
    def loop(remainingIntervals: Seq[FiniteDuration])(mdcData: Map[String, String])(block: => Future[A]): Future[A] =
      // scheduling will loose MDC data. Here we explicitly ensure it is available on block.
      Mdc
        .withMdc(block, mdcData)
        .flatMap(result =>
          if (remainingIntervals.nonEmpty && shouldRetry(Success(result))) {
            val defaultDelay = remainingIntervals.head
            val delay        = delayInterval(Success(result)).getOrElse(defaultDelay)

            Logger(getClass).warn(s"Retrying in $delay due to ${retryReason(Success(result))}")
            after(delay, actorSystem.scheduler)(loop(remainingIntervals.tail)(mdcData)(block))
          } else
            Future.successful(result)
        )
        .recoverWith {
          case e: Throwable =>
            if (remainingIntervals.nonEmpty && shouldRetry(Failure(e))) {
              val defaultDelay = remainingIntervals.head
              val delay        = delayInterval(Failure(e)).getOrElse(defaultDelay)

              Logger(getClass).warn(s"Retrying in $delay due to ${retryReason(Failure(e))}")
              after(delay, actorSystem.scheduler)(loop(remainingIntervals.tail)(mdcData)(block))
            } else
              Future.failed(e)
        }
    loop(intervals)(Mdc.mdcData)(block)
  }

}
