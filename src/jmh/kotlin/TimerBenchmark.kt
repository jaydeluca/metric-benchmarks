package main

import com.codahale.metrics.MetricRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.random.Random

@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
open class TimerBenchmark {

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
    private val valueRange = 20..3000 // Value range, inclusive (represents milliseconds)
    private val dataset = generateDataset(datasetSize, keyRange, valueRange)

    companion object {
        @State(Scope.Benchmark)
        open class DropwizardMetrics {
            val metricRegistry = MetricRegistry()
        }

        @State(Scope.Benchmark)
        open class MicrometerMetrics {
            val meterRegistry = SimpleMeterRegistry()
        }
    }

    @Benchmark
    fun dropwizard(dropwizardMetrics: DropwizardMetrics) {
        dataset.forEach { (frameRenderSpeed, countOfFrames) ->
            repeat(countOfFrames) {
                dropwizardMetrics.metricRegistry.timer("dropwizard.timer")
                    .update(frameRenderSpeed.toLong(), TimeUnit.MILLISECONDS)
            }
        }
    }

    @Benchmark
    fun micrometer(micrometerMetrics: MicrometerMetrics) {
        dataset.forEach { (frameRenderSpeed, countOfFrames) ->
            repeat(countOfFrames) {
                micrometerMetrics.meterRegistry.timer("micrometer.timer")
                    .record(frameRenderSpeed.toLong(), TimeUnit.MILLISECONDS)
            }
        }
    }
}
