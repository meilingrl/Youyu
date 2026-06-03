# Launch Foundation Logging And Health

## Purpose

The staging rehearsal keeps logging console-first and exposes only minimum
health information. It does not introduce metrics collection or a monitoring
stack.

## Health Endpoints

- `GET /api/health` remains available for existing smoke checks.
- `/api/health` returns application status plus a database status probe. It
  must not include JDBC URLs, usernames, hostnames, SQL errors, pool internals,
  secrets, or environment variable values.
- `GET /actuator/health` reports aggregate status plus the `db` and
  `diskSpace` components.
- `/actuator/metrics` and `/actuator/prometheus` are not exposed.
- A failed database probe makes the aggregate Actuator health status unhealthy.

## Trace Logging

`RequestTraceFilter` accepts `X-Trace-Id` when provided or creates one when it
is absent. The trace ID is attached to the request, returned as a response
header, added to MDC during downstream processing, and removed in a `finally`
block after request completion.

The `staging` profile writes logs to stdout and includes `traceId` in the
console pattern. Container logs remain the operational source for this
foundation phase.

Logs must remain request-correlatable but not credential-bearing. Do not log
request bodies for authentication, registration, password reset, payment,
student verification, or admin credential flows. Error handling should keep
stack traces in server logs and return client-safe messages through the normal
API envelope.

## Deferred Work

Metrics, Prometheus, Grafana, business dashboards, alert rules, and external
log aggregation remain deferred to the monitoring phase.
