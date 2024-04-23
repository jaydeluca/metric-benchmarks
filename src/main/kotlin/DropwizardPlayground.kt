package org.metrics

import com.codahale.metrics.MetricRegistry
import java.util.logging.Logger

class DropwizardPlayground {

    companion object {
        val logger: Logger = Logger.getLogger(DropwizardPlayground::class.simpleName)
    }

    fun run(): Runnable {
        logger.info("Starting Dropwizard Playground")
        
        val dropwizardCounter: com.codahale.metrics.Counter = MetricRegistry().counter("test-counter")

        while (true) {
            dropwizardCounter.inc()
            Thread.sleep(1000)
        }
    }
}