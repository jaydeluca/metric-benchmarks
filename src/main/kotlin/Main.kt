package org.metrics

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk
import io.prometheus.client.Counter

fun main() {

    val openTelemetry: OpenTelemetry = AutoConfiguredOpenTelemetrySdk.initialize().openTelemetrySdk

    val meter = openTelemetry.meterBuilder("test-instrument")
        .setInstrumentationVersion("1.0.0")
        .build()

    val attributes: Attributes = Attributes.of(AttributeKey.stringKey("env"), "production")

    val otelCounter = meter.counterBuilder("otel.counter")
        .setUnit("1")
        .build()


    val prometheusCounter: Counter = Counter.Builder()
        .name("test")
        .help("help")
        .labelNames("env")
        .create()

    while (true) {
        prometheusCounter.labels("env").inc()
        otelCounter.add(1, attributes)
    }
    println(prometheusCounter)
    println(otelCounter)
}