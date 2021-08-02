package nationaldutyrepaymentcenter.connectors

import nationaldutyrepaymentcenter.stubs.FileTransferStubs
import nationaldutyrepaymentcenter.support.AppBaseISpec
import play.api.Application
import uk.gov.hmrc.http._
import uk.gov.hmrc.nationaldutyrepaymentcenter.connectors.FileTransferConnector

import scala.concurrent.ExecutionContext.Implicits.global

class FileTransferConnectorISpec extends FileTransferConnectorISpecSetup with FileTransferStubs {

  "FileTransferConnector" when {
    "transferMultipleFiles" should {
      "return 202 if success" in {

        val request = multiFileRequest(conversationId)
        givenFileTransmissionsMultipleSucceeds(request, multiFileResponse(conversationId))
        val result = await(connector.transferMultipleFiles(request))
        result.status mustBe 202
      }
    }
  }
}

trait FileTransferConnectorISpecSetup extends AppBaseISpec with FileTransferStubs {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  override def fakeApplication: Application = appBuilder.build()

  lazy val connector: FileTransferConnector =
    app.injector.instanceOf[FileTransferConnector]

}
