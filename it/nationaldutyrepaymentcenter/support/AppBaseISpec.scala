package nationaldutyrepaymentcenter.support

import nationaldutyrepaymentcenter.stubs.AuthStubs
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application

abstract class AppBaseISpec extends BaseISpec with GuiceOneAppPerSuite with TestApplication with AuthStubs {

  override implicit lazy val app: Application = appBuilder.build()

}
