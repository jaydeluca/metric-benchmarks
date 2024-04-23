package org.metrics

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import java.util.logging.Logger

class MicrometerPlayground {

    companion object {
        val logger: Logger = Logger.getLogger(MicrometerPlayground::class.simpleName)
    }

    fun run(): Runnable {
        logger.info("Starting Micrometer Playground")

        // Micrometer
        val registry = SimpleMeterRegistry()
        val micrometerCounter: io.micrometer.core.instrument.Counter = registry.counter("micrometer.counter")
        val counterWithTags: io.micrometer.core.instrument.Counter = io.micrometer.core.instrument.Counter
            .builder("micrometer.counter2")
            .baseUnit("units")
            .tags("environment", "production", "service", "test")
            .register(registry)

        while (true) {
            micrometerCounter.increment()
            Thread.sleep(1000)
        }
    }
}