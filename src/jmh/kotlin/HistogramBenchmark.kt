package main

import com.codahale.metrics.MetricRegistry
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
import java.util.*
import kotlin.random.Random

@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
open class HistogramBenchmark {

    /**
     * Generates a dataset with random key-value pairs.
     *
     * @param size The size of the dataset.
     * @param keyRange The range of the keys.
     * @param valueRange The range of the values.
     * @return A HashMap with the generated dataset.
     */
    private fun generateDataset(size: Int, keyRange: IntRange, valueRange: IntRange): HashMap<Int, Int> {
        val dataset = HashMap<Int, Int>()

        repeat(size) {
            val key = Random.nextInt(keyRange.first, keyRange.last + 1)
            val value = Random.nextInt(valueRange.first, valueRange.last + 1)

            dataset[key] = value
        }

        return dataset
    }

    private val datasetSize = 100000 // Change this to change the dataset size
    private val keyRange = 0..1000 // Key range, inclusive
    private val valueRange = 20..3000 // Value range, inclusive
    private val dataset = generateDataset(datasetSize, keyRange, valueRange)

    companion object {
        @State(Scope.Benchmark)
        open class DropwizardMetrics {
            val metricRegistry = MetricRegistry()
        }

        @State(Scope.Benchmark)
        open class OTelMetrics {
            val meter: Meter
            init {
                val sdkMeterProvider = SdkMeterProvider.builder()
                    .setResource(Resource.getDefault())
                    .build()
                val openTelemetry: OpenTelemetry = OpenTelemetrySdk.builder()
                    .setMeterProvider(sdkMeterProvider)
                    .build()
                meter = openTelemetry
                    .meterBuilder("benchmark-test-histogram")
                    .setInstrumentationVersion("1.0.0")
                    .build()
            }
        }

        @State(Scope.Benchmark)
        open class SimplePrometheusMetrics {
            // Simple Prometheus client doesn't require state, metrics are registered inline
        }

        @State(Scope.Benchmark)
        open class PrometheusMetrics {
            val registry = PrometheusRegistry()
        }
    }

    @Benchmark
    fun dropwizard(dropwizardMetrics: DropwizardMetrics) {
        dataset.forEach { (frameRenderSpeed, countOfFrames) ->
            repeat(countOfFrames) {
                dropwizardMetrics.metricRegistry.histogram("dropwizard.histogram")
                    .update(frameRenderSpeed.toLong())
            }
        }
    }

    @Benchmark
    fun otel(otelMetrics: OTelMetrics) {
        val histogram = otelMetrics.meter.histogramBuilder("otel.histogram")
            .setDescription("test histogram")
            .setUnit("ms")
            .build()

        dataset.forEach { (frameRenderSpeed, countOfFrames) ->
            repeat(countOfFrames) {
                histogram.record(frameRenderSpeed.toDouble())
            }
        }
    }

    @Benchmark
    fun prometheusSimple(simplePrometheusMetrics: SimplePrometheusMetrics) {
        val histogram = io.prometheus.client.Histogram.build()
            .name("simple_prometheus_histogram")
            .help("help")
            .create()

        dataset.forEach { (frameRenderSpeed, countOfFrames) ->
            repeat(countOfFrames) {
                histogram.observe(frameRenderSpeed.toDouble())
            }
        }
    }

    @Benchmark
    fun prometheus(prometheusMetrics: PrometheusMetrics) {
        val histogram = io.prometheus.metrics.core.metrics.Histogram.builder()
            .name("prometheus_histogram")
            .help("help")
            .register(prometheusMetrics.registry)

        dataset.forEach { (frameRenderSpeed, countOfFrames) ->
            repeat(countOfFrames) {
                histogram.observe(frameRenderSpeed.toDouble())
            }
        }
    }
}
