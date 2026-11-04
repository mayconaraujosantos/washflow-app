package com.washflow.infra.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

/**
 * Process-wide Prometheus registry: HTTP metrics (RequestTracing), the Hikari pool (JdbiFactory)
 * and JVM/process gauges. Scraped through GET /api/metrics.
 */
public final class Metrics {

  private static final PrometheusMeterRegistry REGISTRY = create();

  private Metrics() {}

  public static PrometheusMeterRegistry registry() {
    return REGISTRY;
  }

  private static PrometheusMeterRegistry create() {
    PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    registry.config().commonTags("application", "washflow");
    bindJvm(registry);
    return registry;
  }

  @SuppressWarnings("resource") // JvmGcMetrics lives as long as the process
  private static void bindJvm(MeterRegistry registry) {
    new ClassLoaderMetrics().bindTo(registry);
    new JvmMemoryMetrics().bindTo(registry);
    new JvmGcMetrics().bindTo(registry);
    new JvmThreadMetrics().bindTo(registry);
    new ProcessorMetrics().bindTo(registry);
    new UptimeMetrics().bindTo(registry);
  }
}
