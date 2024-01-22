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

    implementation(platform("io.opentelemetry:opentelemetry-bom:1.34.1"))
    implementation(platform("io.opentelemetry:opentelemetry-bom-alpha:1.34.1-alpha"))
    implementation("io.opentelemetry:opentelemetry-exporter-otlp:1.34.1")

    implementation("io.opentelemetry:opentelemetry-exporter-logging:1.34.1")
    implementation("io.opentelemetry.semconv:opentelemetry-semconv:1.23.1-alpha")

    implementation("io.opentelemetry:opentelemetry-api")
    implementation("io.opentelemetry:opentelemetry-exporter-prometheus")
    implementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")

    implementation("io.prometheus:simpleclient:0.16.0")

}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}

jmh {
    warmupIterations = 1
    iterations = 1
    fork = 1
    profilers.addAll("gc")
}