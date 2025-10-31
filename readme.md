# Metric Benchmarks

Exploring different performance and characteristics of various JVM metrics clients including:
- **Dropwizard Metrics** (`com.codahale.metrics`)
- **Micrometer** (`io.micrometer`)
- **OpenTelemetry** (`io.opentelemetry`)
- **Prometheus Java Client** - both old (`io.prometheus.client`) and new (`io.prometheus.metrics`)

## Running Benchmarks

Run all benchmarks and automatically update this README with results:

```bash
./run-benchmarks.py
# or
./run-benchmarks.sh
```

Or run benchmarks manually without updating README:

```bash
./gradlew jmh
```

To update README with existing results (skip benchmark execution):

```bash
./run-benchmarks.py --skip-run
```



## Benchmark Results

*Last updated: 2025-10-31 08:47:10*

Run benchmarks with: `./run-benchmarks.py` or `./gradlew jmh`

### Counter Benchmarks

| Library                    | Throughput (ops/s) | Allocation per op | GC Count |
| -------------------------- | -----------------: | ----------------: | -------: |
| OpenTelemetry + attributes |              1.56B |              ~0 B |       ~0 |
| OpenTelemetry              |              1.53B |              ~0 B |       ~0 |
| Dropwizard                 |              1.98K |            0.02 B |       ~0 |
| Prometheus (new client)    |              1.94K |            1.12 B |       ~0 |
| Micrometer                 |              1.60K |            0.03 B |       ~0 |
| Micrometer + tags          |              1.44K |            0.03 B |       ~0 |
| Prometheus (new) + labels  |             792.88 |           6.40M B |   161.00 |
| Prometheus (old client)    |             394.80 |            0.14 B |       ~0 |
| Prometheus (old) + labels  |             359.16 |           4.80M B |    55.00 |

### Gauge Benchmarks

| Library                 | Throughput (ops/s) | Allocation per op | GC Count |
| ----------------------- | -----------------: | ----------------: | -------: |
| Prometheus (old client) |              2.00K |           4.00M B |    63.00 |
| Dropwizard              |              1.83K |           4.00M B |    63.00 |
| OpenTelemetry           |              1.81K |           4.00M B |    67.00 |
| Micrometer              |              1.81K |           4.00M B |    56.00 |
| Prometheus (new client) |              1.75K |           4.10M B |    65.00 |

### Timer Benchmarks

| Library    | Throughput (ops/s) | Allocation per op | GC Count |
| ---------- | -----------------: | ----------------: | -------: |
| Micrometer |              24.36 |          60.99M B |    47.00 |
| Dropwizard |               7.56 |          49.15M B |    12.00 |

### Histogram Benchmarks

| Library                 | Throughput (ops/s) | Allocation per op | GC Count |
| ----------------------- | -----------------: | ----------------: | -------: |
| OpenTelemetry           |            461.32K |              ~0 B |       ~0 |
| Prometheus (old client) |              26.40 |           1.87K B |       ~0 |
| Dropwizard              |              11.11 |          47.42M B |    17.00 |

**Notes:**
- Throughput: Higher is better (operations per second)
- Allocation per op: Lower is better (bytes allocated per operation)
- GC Count: Lower is better (number of garbage collections during test)
- B = Billion, M = Million, K = Thousand


## Clickhouse

```sql


/* identify all tables */
SELECT
    table,
    sum(rows) AS rows,
    max(modification_time) AS latest_modification,
    formatReadableSize(sum(bytes)) AS data_size,
    formatReadableSize(sum(primary_key_bytes_in_memory)) AS primary_keys_size,
    any(engine) AS engine,
    sum(bytes) AS bytes_size
FROM clusterAllReplicas(default, system.parts)
WHERE active
GROUP BY
    database,
    table
ORDER BY bytes_size DESC;

/* See schema */
SHOW CREATE TABLE otel.otel_metrics_histogram

/* Query all */
SELECT * FROM otel.otel_metrics_histogram;

/* Query specific Fields */

SELECT MetricName, Attributes, StartTimeUnix, Value FROM otel.otel_metrics_exponential_histogram;

```