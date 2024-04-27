package org.metrics

import io.prometheus.client.Counter
import java.util.logging.Logger

class PrometheusPlayground {

    companion object {
        val logger: Logger = Logger.getLogger(PrometheusPlayground::class.simpleName)
    }

    fun run(): Runnable {
        logger.info("Starting Prometheus Playground")

        val prometheusCounter: Counter = Counter.Builder()
            .name("test")
            .help("help")
            .labelNames("env")
            .create()

        while (true) {
            prometheusCounter.labels("env").inc()
            Thread.sleep(1000)
        }
    }
}