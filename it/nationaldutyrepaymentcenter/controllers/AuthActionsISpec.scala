package nationaldutyrepaymentcenter.controllers

import nationaldutyrepaymentcenter.support.AppBaseISpec
import play.api.mvc.Result
import play.api.mvc.Results._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisationException}
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}
import uk.gov.hmrc.nationaldutyrepaymentcenter.controllers.AuthActions
import uk.gov.hmrc.nationaldutyrepaymentcenter.wiring.{AppConfig, AppConfigImpl}

import scala.concurrent.Future

class AuthActionsISpec extends AppBaseISpec {

  object TestController extends AuthActions {

    override def authConnector: AuthConnector = app.injector.instanceOf[AuthConnector]

    override val appConfig: AppConfig = app.injector.instanceOf[AppConfigImpl]

    implicit val hc      = HeaderCarrier()
    implicit val request = FakeRequest().withSession(SessionKeys.authToken -> "Bearer XYZ")

    import scala.concurrent.ExecutionContext.Implicits.global

    def withAuthorised[A]: Future[Result] =
      super.withAuthorised {
        Future.successful(Ok("Hello!"))
      }

  }

  "withAuthorised" should {

    "call body when user is authorized" in {
      stubForAuthAuthorise("{}", "{}")
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
