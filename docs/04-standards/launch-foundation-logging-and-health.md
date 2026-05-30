# Launch Foundation Logging And Health

## Purpose

The staging rehearsal keeps logging console-first and exposes only minimum
health information. It does not introduce metrics collection or a monitoring
stack.

## Health Endpoints

- `GET /api/health` remains available for existing smoke checks.
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

## Deferred Work

Metrics, Prometheus, Grafana, business dashboards, alert rules, and external
log aggregation remain deferred to the monitoring phase.
