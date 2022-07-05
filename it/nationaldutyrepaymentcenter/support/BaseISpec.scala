package nationaldutyrepaymentcenter.support

import akka.stream.Materializer
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.Application
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.duration.{Duration, DurationInt, FiniteDuration}
import scala.concurrent.{Await, Future}

abstract class BaseISpec extends AnyWordSpec with Matchers with WireMockSupport with MetricsTestSupport {

  def app: Application

  override def commonStubs(): Unit =
    givenCleanMetricRegistry()

  implicit val defaultTimeout: FiniteDuration = 5 seconds

  def await[A](future: Future[A])(implicit timeout: Duration): A = Await.result(future, timeout)

  protected implicit lazy val materializer: Materializer = app.materializer

  private lazy val messagesApi = app.injector.instanceOf[MessagesApi]

  private implicit lazy val messages: Messages = messagesApi.preferred(Seq.empty[Lang])

  protected def htmlEscapedMessage(key: String): String = HtmlFormat.escape(Messages(key)).toString

  implicit def hc(implicit request: FakeRequest[_]): HeaderCarrier = {
    val authorisedRequest = request.withHeaders("Authorisation" -> "dummy-bearer-token")

    HeaderCarrierConverter.fromRequestAndSession(authorisedRequest, authorisedRequest.session)
  }

}
