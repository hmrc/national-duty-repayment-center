package nationaldutyrepaymentcenter.connectors

import play.api.Application
import nationaldutyrepaymentcenter.support.AppBaseISpec
import uk.gov.hmrc.nationaldutyrepaymentcenter.models._
import uk.gov.hmrc.http._

import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.json.Json
import uk.gov.hmrc.nationaldutyrepaymentcenter.connectors.FileTransferConnector
import nationaldutyrepaymentcenter.stubs.FileTransferStubs


class FileTransferConnectorISpec extends FileTransferConnectorISpecSetup with FileTransferStubs {

  "FileTransferConnector" when {
    "transferFile" should {
      "return 200 if success" in {

        val request = testRequest
        givenNdrcFileTransferSucceeds(request)
        val result = await(connector.transferFile(request))
        result.success mustBe true
      }
    }
  }
}

trait FileTransferConnectorISpecSetup extends AppBaseISpec with FileTransferStubs {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  override def fakeApplication: Application = appBuilder.build()

  lazy val connector: FileTransferConnector =
    app.injector.instanceOf[FileTransferConnector]

  val correlationId: String = java.util.UUID.randomUUID().toString
  val conversationId: String = java.util.UUID.randomUUID().toString

  val testRequest: FileTransferRequest = Json
    .parse(s"""{
              |"conversationId":"$conversationId",
              |"caseReferenceNumber":"Risk-123",
              |"applicationName":"NDRC",
              |"upscanReference":"XYZ0123456789",
              |"downloadUrl":"/dummy.jpeg",
              |"fileName":"dummy.jpeg",
              |"fileMimeType":"image/jpeg",
              |"checksum":"${"0" * 64}",
              |"batchSize": 1,
              |"batchCount": 1,
              |"correlationId": "$correlationId"
              |}""".stripMargin)
    .as[FileTransferRequest]

}
