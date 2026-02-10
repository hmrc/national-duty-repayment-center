/*
 * Copyright 2026 HM Revenue & Customs
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

package nationaldutyrepaymentcenter.controllers

import nationaldutyrepaymentcenter.support.AppBaseISpec
import play.api.mvc.Result
import play.api.mvc.AnyContent
import play.api.mvc.Results._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisationException}
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}
import uk.gov.hmrc.nationaldutyrepaymentcenter.controllers.AuthActions
import uk.gov.hmrc.nationaldutyrepaymentcenter.wiring.{AppConfig, AppConfigImpl}
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.Future

class AuthActionsISpec extends AppBaseISpec {

  object TestController extends AuthActions {

    override def authConnector: AuthConnector = app.injector.instanceOf[AuthConnector]

    override val appConfig: AppConfig = app.injector.instanceOf[AppConfigImpl]

    implicit val request: FakeRequest[AnyContent] = FakeRequest().withSession(SessionKeys.authToken -> "Bearer XYZ")
    println(request)
    
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    import scala.concurrent.ExecutionContext.Implicits.global

    def withAuthorised[A]: Future[Result] =
      super.withAuthorised {
        Future.successful(Ok("Hello!"))
      }

  }

  "withAuthorised" should {

    "call body when user is authorized" in {
      stubForAuthAuthorise("{}")
      val result = TestController.withAuthorised

      status(result) mustBe 200
      contentAsString(result) mustBe "Hello!"
    }

    "throw an AutorisationException when user not logged in" in {
      givenUnauthorisedWith("MissingBearerToken")
      an[AuthorisationException] shouldBe thrownBy {
        await(TestController.withAuthorised)
      }
    }
  }

}
