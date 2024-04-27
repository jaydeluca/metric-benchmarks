package main

import com.codahale.metrics.MetricRegistry
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
class TimerAllocations {

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

    @Benchmark
    fun timers() {
        val metricRegistry = MetricRegistry()

        dataset.forEach { (frameRenderSpeed, countOfFrames) ->
            repeat(countOfFrames) {
                metricRegistry.timer("time")
                    .update(frameRenderSpeed.toLong(), TimeUnit.MILLISECONDS)
            }
        }
    }

    @Benchmark
    fun histogram() {
        val metricRegistry = MetricRegistry()

        dataset.forEach { (frameRenderSpeed, countOfFrames) ->
            repeat(countOfFrames) {
                metricRegistry.timer("time")
                    .update(frameRenderSpeed.toLong(), TimeUnit.MILLISECONDS)
            }
        }
    }

}