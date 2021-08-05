package nationaldutyrepaymentcenter.support

import com.kenshoo.play.metrics.Metrics
import org.scalatest.Suite
import org.scalatest.matchers.must.Matchers
import play.api.Application

import scala.collection.JavaConverters

trait MetricsTestSupport {
  self: Suite with Matchers =>

  def app: Application

  def givenCleanMetricRegistry(): Unit = {
    val registry = app.injector.instanceOf[Metrics].defaultRegistry
    for (
      metric <- JavaConverters
        .asScalaIterator[String](registry.getMetrics.keySet().iterator())
    )
      registry.remove(metric)
  }

}
