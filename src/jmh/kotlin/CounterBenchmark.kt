package main

import com.codahale.metrics.MetricRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.metrics.LongCounter
import io.opentelemetry.api.metrics.Meter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.resources.Resource
import io.prometheus.client.Counter
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State


@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
open class CounterBenchmark {

    private val otelAttributes: Attributes = Attributes.of(
        AttributeKey.stringKey("environment"), "production",
        AttributeKey.stringKey("service"), "test"
    )
    private val repetitions = 100000

    companion object {
        @State(Scope.Benchmark)
        open class DropwizardCounter {
            val counter: com.codahale.metrics.Counter = MetricRegistry().counter("test-counter")
        }

        @State(Scope.Benchmark)
        open class MicrometerCounter {
            private val registry = SimpleMeterRegistry()
            val counter: io.micrometer.core.instrument.Counter = registry.counter("micrometer.counter")
            val counterWithTags: io.micrometer.core.instrument.Counter = io.micrometer.core.instrument.Counter
                .builder("micrometer.counter2")
                .baseUnit("units")
                .tags("environment", "production", "service", "test")
                .register(registry)
        }

        @State(Scope.Benchmark)
        open class PrometheusCounter {
            val counter: Counter = Counter.Builder()
                .name("test")
                .help("help")
                .create()

            val counterWithLabels: Counter = Counter.Builder()
                .name("test2")
                .help("help")
                .labelNames("environment", "service")
                .create()
        }


        @State(Scope.Benchmark)
        open class OTelCounter {
            var counter: LongCounter
            init {
                val sdkMeterProvider = SdkMeterProvider.builder()
                    .setResource(Resource.getDefault())
                    .build()
                val openTelemetry: OpenTelemetry = OpenTelemetrySdk.builder()
                    .setMeterProvider(sdkMeterProvider)
                    .build()
                val meter: Meter = openTelemetry
                    .meterBuilder("instrumentation-library-name")
                    .setInstrumentationVersion("1.0.0")
                    .build()
                this.counter = meter
                    .counterBuilder("test1")
                    .setDescription("test")
                    .build()
            }
        }
    }

    @Benchmark
    fun dropwizard(dropwizardCounter: DropwizardCounter): com.codahale.metrics.Counter? {
        repeat(repetitions) {
            dropwizardCounter.counter.inc()
        }
        return dropwizardCounter.counter
    }

    @Benchmark
    fun micrometer(micrometerCounter: MicrometerCounter): io.micrometer.core.instrument.Counter? {
        repeat(repetitions) {
            micrometerCounter.counter.increment()
        }
        return micrometerCounter.counter
    }

    @Benchmark
    fun micrometerWithTags(micrometerCounter: MicrometerCounter): io.micrometer.core.instrument.Counter? {
        repeat(repetitions) {
            micrometerCounter.counterWithTags.increment()
        }
        return micrometerCounter.counterWithTags
    }

    @Benchmark
    fun otel(otelCounter: OTelCounter): LongCounter {
        repeat(repetitions) {
            otelCounter.counter.add(1)
        }
        return otelCounter.counter
    }

    @Benchmark
    fun otelWithAttributes(otelCounter: OTelCounter): LongCounter {
        repeat(repetitions) {
            otelCounter.counter.add(1, otelAttributes)
        }
        return otelCounter.counter
    }

    @Benchmark
    fun prometheus(prometheusCounter: PrometheusCounter): Counter? {
        repeat(repetitions) {
            prometheusCounter.counter.inc()
        }
        return prometheusCounter.counter
    }

    @Benchmark
    fun prometheusWithLabels(prometheusCounter: PrometheusCounter): Counter? {
        repeat(repetitions) {
            prometheusCounter.counterWithLabels.labels("production", "test").inc()
        }
        return prometheusCounter.counterWithLabels
    }
}
