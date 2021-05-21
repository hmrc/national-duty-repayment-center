package nationaldutyrepaymentcenter.controllers

import nationaldutyrepaymentcenter.support.AppBaseISpec
import play.api.mvc.Result
import play.api.mvc.Results._
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisationException, InsufficientEnrolments}
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}
import uk.gov.hmrc.nationaldutyrepaymentcenter.controllers.AuthActions
import uk.gov.hmrc.nationaldutyrepaymentcenter.wiring.AppConfig
import play.api.test.Helpers._

import scala.concurrent.Future

class AuthActionsISpec extends AppBaseISpec {

  object TestController extends AuthActions {

    override def authConnector: AuthConnector = app.injector.instanceOf[AuthConnector]

    override val appConfig: AppConfig = new AppConfig {
      override val appName: String = ""
      override val authBaseUrl: String = ""
      override val eisBaseUrl: String = ""
      override val eisCreateCaseApiPath: String = ""
      override val eisAmendCaseApiPath: String = ""
      override val eisAuthorizationToken: String = ""
      override val eisEnvironment: String = ""
      override val fileBaseUrl: String = ""
      override val fileBasePath: String = ""
    }

    implicit val hc = HeaderCarrier()
    implicit val request = FakeRequest().withSession(SessionKeys.authToken -> "Bearer XYZ")
    import scala.concurrent.ExecutionContext.Implicits.global

    def withAuthorised[A]: Future[Result] =
      super.withAuthorised {
        Future.successful(Ok("Hello!"))
      }

  }

  "withAuthorised" should {

    "call body when user is authorized" in {
      stubForAuthAuthorise(
        "{}",
        "{}"
      )
      val result = TestController.withAuthorised
      status(result) shouldBe 200
      contentAsString(result) shouldBe "Hello!"
    }

    "throw an AutorisationException when user not logged in" in {
      givenUnauthorisedWith("MissingBearerToken")
      an[AuthorisationException] shouldBe thrownBy {
        TestController.withAuthorised
      }
    }
  }

}
