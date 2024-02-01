package nationaldutyrepaymentcenter.support

import uk.gov.hmrc.play.bootstrap.metrics.Metrics
import org.scalatest.Suite
import org.scalatest.matchers.must.Matchers
import play.api.Application

import scala.jdk.CollectionConverters._

trait MetricsTestSupport {
  self: Suite with Matchers =>

  def app: Application

  def givenCleanMetricRegistry(): Unit = {
    val registry = app.injector.instanceOf[Metrics].defaultRegistry
    for (metric <- registry.getMetrics.keySet().iterator().asScala)
      registry.remove(metric)
  }

}
