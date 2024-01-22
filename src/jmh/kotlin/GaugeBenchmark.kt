package main

import com.codahale.metrics.Gauge
import com.codahale.metrics.MetricRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State
import java.util.LinkedList

@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
open class GaugeBenchmark : Registries() {

    private val repetitions = 100000

    @Benchmark
    fun dropwizard() {
        val metricRegistry = MetricRegistry()
        val queue = LinkedList<Int>()

        metricRegistry.register(
            MetricRegistry.name("dropwizard.gauge", "size"),
            Gauge { queue.size }
        )

        for (i in 1..repetitions) {
            queue.add(i)
        }

        while (queue.isNotEmpty()) {
            queue.remove()
        }
    }

    @Benchmark
    fun micrometer() {
        val meterRegistry = SimpleMeterRegistry()
        val queue = LinkedList<Int>()

        io.micrometer.core.instrument.Gauge
            .builder("micrometer.gauge", queue::size)
            .register(meterRegistry)

        for (i in 1..repetitions) {
            queue.add(i)
        }

        while (queue.isNotEmpty()) {
            queue.remove()
        }
    }

    @Benchmark
    fun otel() {
        val meter = createOtelMeter("gauge")
        val queue = LinkedList<Int>()

        meter.gaugeBuilder("otel.gauge")
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
}
