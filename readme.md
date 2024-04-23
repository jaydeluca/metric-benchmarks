# Metric Benchmarks

Exploring different performance and characteristics of various JVM metrics clients.



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