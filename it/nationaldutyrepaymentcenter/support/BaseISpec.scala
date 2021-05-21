package nationaldutyrepaymentcenter.support

import akka.stream.Materializer
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, OptionValues, WordSpecLike}
import play.api.Application
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.Future

abstract class BaseISpec extends WordSpecLike with Matchers with OptionValues with ScalaFutures
  with WireMockSupport with MetricsTestSupport {

  def app: Application

  override def commonStubs(): Unit = {
    givenCleanMetricRegistry()
  }

  protected implicit def materializer: Materializer = app.materializer

  protected def checkHtmlResultWithBodyText(result: Future[Result], expectedSubstring: String): Unit = {
    status(result) shouldBe 200
    contentType(result) shouldBe Some("text/html")
    charset(result) shouldBe Some("utf-8")
    contentAsString(result) should include(expectedSubstring)
  }

  private lazy val messagesApi = app.injector.instanceOf[MessagesApi]
  private implicit def messages: Messages = messagesApi.preferred(Seq.empty[Lang])

  protected def htmlEscapedMessage(key: String): String = HtmlFormat.escape(Messages(key)).toString

  implicit def hc(implicit request: FakeRequest[_]): HeaderCarrier =
    HeaderCarrierConverter.fromRequestAndSession(request, request.session)

}
