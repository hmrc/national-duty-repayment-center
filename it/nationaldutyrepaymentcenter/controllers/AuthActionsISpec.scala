package nationaldutyrepaymentcenter.controllers

import nationaldutyrepaymentcenter.support.AppBaseISpec
import play.api.mvc.Result
import play.api.mvc.Results._
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisationException, InsufficientEnrolments}
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}
import uk.gov.hmrc.nationaldutyrepaymentcenter.config.AppConfig
import uk.gov.hmrc.nationaldutyrepaymentcenter.controllers.AuthActions

import scala.concurrent.Future

class AuthActionsISpec extends AppBaseISpec {

  object TestController extends AuthActions {

    override def authConnector: AuthConnector = app.injector.instanceOf[AuthConnector]

    override val appConfig: AppConfig = new AppConfig {
      override val createCaseApiAuthorizationToken: String = ""
      override val createCaseApiEnvironment: String = ""
      override val graphiteHost: String = ""
      override val createCaseBaseUrl: String = ""
    }

    implicit val hc = HeaderCarrier()
    implicit val request = FakeRequest().withSession(SessionKeys.authToken -> "Bearer XYZ")
    import scala.concurrent.ExecutionContext.Implicits.global

    def withAuthorised[A]: Result =
      await(super.withAuthorised {
        Future.successful(Ok("Hello!"))
      })

  }

  "withAuthorised" should {

    "call body when user is authorized" in {
      stubForAuthAuthorise(
        "{}",
        "{}"
      )
      val result = TestController.withAuthorised
      status(result) shouldBe 200
      bodyOf(result) shouldBe "Hello!"
    }

    "throw an AutorisationException when user not logged in" in {
      givenUnauthorisedWith("MissingBearerToken")
      an[AuthorisationException] shouldBe thrownBy {
        TestController.withAuthorised
      }
    }
  }

}
