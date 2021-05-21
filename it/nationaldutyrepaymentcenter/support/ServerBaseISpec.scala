package nationaldutyrepaymentcenter.support

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application

abstract class ServerBaseISpec extends BaseISpec with GuiceOneAppPerSuite with TestApplication with ScalaFutures {

  override lazy val app: Application = appBuilder.build()

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(4, Seconds), interval = Span(1, Seconds))

}
