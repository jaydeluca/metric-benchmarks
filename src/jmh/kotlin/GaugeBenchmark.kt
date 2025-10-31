package main

import com.codahale.metrics.Gauge
import com.codahale.metrics.MetricRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.metrics.Meter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.resources.Resource
import io.prometheus.metrics.model.registry.PrometheusRegistry
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State
import java.util.LinkedList
import java.util.concurrent.atomic.AtomicInteger

@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
open class GaugeBenchmark {

    private val repetitions = 100000

    companion object {
        @State(Scope.Benchmark)
        open class DropwizardGauge {
            val metricRegistry = MetricRegistry()
        }

        @State(Scope.Benchmark)
        open class MicrometerGauge {
            val meterRegistry = SimpleMeterRegistry()
        }

        @State(Scope.Benchmark)
        open class OTelGauge {
            val meter: Meter
            init {
                val sdkMeterProvider = SdkMeterProvider.builder()
                    .setResource(Resource.getDefault())
                    .build()
                val openTelemetry: OpenTelemetry = OpenTelemetrySdk.builder()
                    .setMeterProvider(sdkMeterProvider)
                    .build()
                meter = openTelemetry
                    .meterBuilder("benchmark-test-gauge")
                    .setInstrumentationVersion("1.0.0")
                    .build()
            }
        }

        @State(Scope.Benchmark)
        open class SimplePrometheusGauge {
            // Simple Prometheus client doesn't require state, gauges are registered inline
        }

        @State(Scope.Benchmark)
        open class PrometheusGauge {
            val registry = PrometheusRegistry()
        }
    }

    @Benchmark
    fun dropwizard(dropwizardGauge: DropwizardGauge) {
        val queue = LinkedList<Int>()
        val gaugeName = MetricRegistry.name("dropwizard.gauge", "size", System.nanoTime().toString())

        dropwizardGauge.metricRegistry.register(
            gaugeName,
            Gauge { queue.size }
        )

        for (i in 1..repetitions) {
            queue.add(i)
        }

        while (queue.isNotEmpty()) {
            queue.remove()
        }

        dropwizardGauge.metricRegistry.remove(gaugeName)
    }

    @Benchmark
    fun micrometer(micrometerGauge: MicrometerGauge) {
        val queue = LinkedList<Int>()

        io.micrometer.core.instrument.Gauge
            .builder("micrometer.gauge", queue::size)
            .register(micrometerGauge.meterRegistry)

        for (i in 1..repetitions) {
            queue.add(i)
        }

        while (queue.isNotEmpty()) {
            queue.remove()
        }
    }

    @Benchmark
    fun otel(otelGauge: OTelGauge) {
        val queue = LinkedList<Int>()

        otelGauge.meter.gaugeBuilder("otel.gauge")
            .setUnit("1")
            .buildWithCallback {
                it.record(queue.size.toDouble())
            }

        for (i in 1..repetitions) {
            queue.add(i)
        }

        while (queue.isNotEmpty()) {
            queue.remove()
        }
    }

    @Benchmark
    fun prometheusSimple(simplePrometheusGauge: SimplePrometheusGauge) {
        val queue = LinkedList<Int>()

        val gauge = io.prometheus.client.Gauge.build()
            .name("simple_prometheus_gauge_${System.nanoTime()}")
            .help("help")
            .create()

        for (i in 1..repetitions) {
            queue.add(i)
            gauge.set(queue.size.toDouble())
        }

        while (queue.isNotEmpty()) {
            queue.remove()
            gauge.set(queue.size.toDouble())
        }
    }

    @Benchmark
    fun prometheus(prometheusGauge: PrometheusGauge) {
        val queue = LinkedList<Int>()

        val gauge = io.prometheus.metrics.core.metrics.Gauge.builder()
            .name("prometheus_gauge_${System.nanoTime()}")
            .help("help")
            .register(prometheusGauge.registry)

        for (i in 1..repetitions) {
            queue.add(i)
            gauge.set(queue.size.toDouble())
        }

        while (queue.isNotEmpty()) {
            queue.remove()
            gauge.set(queue.size.toDouble())
        }
    }
}
