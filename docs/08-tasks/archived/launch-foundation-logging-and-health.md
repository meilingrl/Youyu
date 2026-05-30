# Task: Launch Foundation Logging And Health

## Metadata

- ID: launch-foundation-logging-and-health
- Status: completed
- Owner: worker-f
- Track: cross-cutting
- Depends on: launch-foundation-containerization, launch-foundation-production-config
- Priority: high
- Planned date: 2026-05-30
- Completed date: 2026-05-30

## Objective

Add minimum Spring Boot Actuator health and console-first traceable logging for staging rehearsal.

## Scope

- add Actuator dependency and minimum health exposure
- keep `/api/health` compatibility
- allow `/actuator/health` through auth filtering
- populate and clear MDC traceId in `RequestTraceFilter`
- configure staging console logging

## Out of Scope

- metrics, Prometheus, Grafana, business monitoring

## Allowed Changes

- `backend/pom.xml`
- backend health/auth filter configuration
- `backend/src/main/java/com/youyu/backend/filter/RequestTraceFilter.java`
- health-focused tests and documentation

## Acceptance Criteria

- [ ] `/api/health` remains available.
- [ ] `/actuator/health` is available with DB health.
- [ ] Metrics and Prometheus endpoints are not exposed.
- [ ] traceId enters MDC and is cleared after the request.

## Completion Notes

Added minimal Actuator health exposure, database health, console traceId logging,
and MDC cleanup coverage. Backend suite passed with 162 tests. A database outage
did not report `UP`, but JDBC health response timeout tuning remains follow-up.
