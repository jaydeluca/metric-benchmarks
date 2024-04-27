package org.metrics

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.exporter.logging.LoggingMetricExporter
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.metrics.Aggregation
import io.opentelemetry.sdk.metrics.InstrumentSelector
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.metrics.View
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader
import java.time.Duration
import java.util.logging.Logger
import kotlin.random.Random

class OtelPlayground {

    companion object {
        val logger: Logger = Logger.getLogger(OtelPlayground::class.simpleName)
    }

    fun run(): Runnable {
        logger.info("Starting Otel Playground")

        val sdkMeterProvider =
            SdkMeterProvider.builder()
                .registerView( // Target histograms matching this name and apply a custom maxScale
                    InstrumentSelector.builder().setName("*custom_scale*").build(),
                    View.builder()
                        .setAggregation(Aggregation.base2ExponentialBucketHistogram(160, 4))
                        .build()
                )
                .registerMetricReader(
                    PeriodicMetricReader.builder(LoggingMetricExporter.create())
                        .setInterval(Duration.ofSeconds(10)).build()
                )
                .registerMetricReader(
                    PeriodicMetricReader.builder(OtlpGrpcMetricExporter.builder().build())
                        .setInterval(Duration.ofSeconds(10)).build()
                )
                .build()

        val openTelemetry: OpenTelemetry = OpenTelemetrySdk.builder().setMeterProvider(sdkMeterProvider).build()

        val meter = openTelemetry.meterBuilder("test-instrument")
            .setInstrumentationVersion("1.0.0")
            .build()

        val otelCounter = meter.counterBuilder("otel.counter")
            .setUnit("1")
            .build()

        val histogram =
            meter
                .histogramBuilder("job.duration")
                .ofLongs()
                .setDescription("A distribution of job execution time")
                .setUnit("seconds")
                .build()

        val customScaleHistogram =
            meter
                .histogramBuilder("job2.custom_scale.duration")
                .ofLongs()
                .setDescription("A distribution of job2's execution time using a custom scale value.")
                .setUnit("seconds")
                .build()

        val attrs = Attributes.of(stringKey("job"), "update_database")
        val attributes: Attributes = Attributes.of(stringKey("env"), "production")

        while (true) {
            val value = Random.nextLong(1, 10)
            otelCounter.add(value, attributes)
            histogram.record(Random.nextLong(value), attrs)
            customScaleHistogram.record(Random.nextLong(value), attrs)
            Thread.sleep(1000)
        }
    }
}