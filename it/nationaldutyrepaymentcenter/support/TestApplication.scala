package nationaldutyrepaymentcenter.support

import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar.mock
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.nationaldutyrepaymentcenter.controllers.UUIDGenerator

trait TestApplication {
  _: BaseISpec =>

  override implicit lazy val app: Application = appBuilder.build()
  val uuideGeneratorMock = mock[UUIDGenerator]

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.auth.port" -> wireMockPort,
        "microservice.services.eis.createcaseapi.host" -> wireMockHost,
        "microservice.services.eis.createcaseapi.port" -> wireMockPort,
        "microservice.services.eis.createcaseapi.token" -> "dummy-it-token",
        "microservice.services.eis.createcaseapi.environment" -> "it",
        "metrics.enabled" -> true,
        "auditing.enabled" -> false,
        "auditing.consumer.baseUri.host" -> wireMockHost,
        "auditing.consumer.baseUri.port" -> wireMockPort,
        "microservice.services.file-transfer.host" -> wireMockHost,
        "microservice.services.file-transfer.port" -> wireMockPort,
      )  .overrides(
      bind[UUIDGenerator].toInstance(uuideGeneratorMock))
}
