package main

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.metrics.Meter
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk

open class Registries {

    fun createOtelMeter(name: String): Meter {
        val openTelemetry: OpenTelemetry = AutoConfiguredOpenTelemetrySdk.initialize().openTelemetrySdk
        return openTelemetry.meterBuilder("benchmark-test-$name")
            .setInstrumentationVersion("1.0.0")
            .build()
    }
}