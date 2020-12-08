package nationaldutyrepaymentcenter.support

import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.Metrics
import org.scalatest.{Matchers, Suite}
import play.api.Application

import scala.collection.JavaConverters

trait MetricsTestSupport {
  self: Suite with Matchers =>

  def app: Application

  private var metricsRegistry: MetricRegistry = _

  def givenCleanMetricRegistry(): Unit = {
    val registry = app.injector.instanceOf[Metrics].defaultRegistry
    for (
      metric <- JavaConverters
                  .asScalaIterator[String](registry.getMetrics.keySet().iterator())
    )
      registry.remove(metric)
    metricsRegistry = registry
  }

}
