import org.openjdk.jmh.profile.GCProfiler

plugins {
    kotlin("jvm") version "1.9.21"
    `java-library`
    id("me.champeau.jmh") version "0.7.2"
}

group = "org.metrics"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    implementation("io.dropwizard.metrics:metrics-core:4.2.0")
    implementation("io.micrometer:micrometer-registry-statsd:latest.release")

    // OpenTelemetry BOM - manages versions for all OpenTelemetry dependencies
    implementation(platform("io.opentelemetry:opentelemetry-bom:1.55.0"))

    // OpenTelemetry dependencies - versions managed by BOM
    implementation("io.opentelemetry:opentelemetry-api")
    implementation("io.opentelemetry:opentelemetry-sdk")
    implementation("io.opentelemetry:opentelemetry-sdk-metrics")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")
    implementation("io.opentelemetry:opentelemetry-exporter-logging")
    implementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")

    implementation("io.prometheus:simpleclient:0.16.0")
    implementation("io.prometheus:prometheus-metrics-core:1.4.2")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

jmh {
    warmupIterations = 1
    iterations = 1
    fork = 1
    profilers.addAll("gc")
}