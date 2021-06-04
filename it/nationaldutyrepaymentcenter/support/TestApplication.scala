package nationaldutyrepaymentcenter.support

import org.scalatest.mockito.MockitoSugar.mock
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.nationaldutyrepaymentcenter.services.UUIDGenerator

import java.time.{Clock, Instant, ZoneId}

trait TestApplication {
  _: BaseISpec =>

  override implicit lazy val app: Application = appBuilder.build()
  val uuidGeneratorMock: UUIDGenerator = mock[UUIDGenerator]
  val clock: Clock = Clock.fixed(Instant.parse("2020-09-09T10:15:30.00Z"), ZoneId.of("UTC"))

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
      bind[Clock].toInstance(clock),
      bind[UUIDGenerator].toInstance(uuidGeneratorMock))
}
