package nationaldutyrepaymentcenter.support

import nationaldutyrepaymentcenter.stubs.AuthStubs
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application

abstract class AppBaseISpec extends BaseISpec with OneAppPerSuite with TestApplication with AuthStubs {

  override implicit lazy val app: Application = appBuilder.build()

}
