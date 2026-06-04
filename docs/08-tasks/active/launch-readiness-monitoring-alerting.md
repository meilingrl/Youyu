# Task: Launch Readiness Monitoring Alerting

## Metadata

- ID: launch-readiness-monitoring-alerting
- Status: active
- Owner: Codex
- Track: cross-cutting
- Depends on: launch-preparation roadmap L6; runtime infrastructure wave 1; deployment runbook task
- Priority: P1
- Planned date: 2026-06-04
- Completed date:

## Objective

Add or document the monitoring, alerting, log-search, and incident-response baseline needed to operate the launch candidate.

## Background

The backend exposes health behavior and request trace IDs, but launch readiness still needs application metrics, business signals, alert thresholds, dashboard/runbook ownership, log-search procedures, and incident response steps that do not rely on developer-local environments.

## Current State

Completed in earlier launch-preparation waves:

- `/api/health` exists and includes a database status probe.
- `/actuator/health` is exposed with limited details and db/disk health.
- Request trace IDs are attached to requests, responses, and backend MDC logs.
- The staging logging pattern includes `traceId`.
- The logging/health foundation document explicitly keeps `/actuator/metrics` and `/actuator/prometheus` unexposed.

Remaining work:

- Metrics collection, Prometheus/Grafana or another monitoring stack, external log aggregation, alert thresholds, alert delivery, and incident ownership are not implemented.
- Business-signal monitoring for payment/order/support/report flows needs either implementation or explicit deferment with owners.

## Scope

- Expose or document application metrics for request rate, latency, error rate, JVM, database pool, and Redis health when enabled.
- Define business metrics for registration, login, order, payment, refund, report, and support flows.
- Add alert rules or runbook thresholds for service down, high error rate, DB pool exhaustion, disk pressure, backup failure, and payment abnormality.
- Document log search by traceId, user ID, order ID, payment ID, and report/support IDs.
- Define incident severity, ownership, acknowledgement, escalation, and post-incident record expectations.

## Out of Scope

- Buying or configuring a real hosted monitoring service.
- Full BI dashboards or product analytics expansion.
- Replacing existing frontend admin dashboard metrics.
- Guaranteeing alert delivery without a configured external channel.

## Files to Read

- `docs/04-standards/operations-and-deployment.md`
- `docs/04-standards/launch-foundation-logging-and-health.md`
- `docs/04-standards/launch-readiness-monitoring-alerting.md`
- `docs/04-standards/launch-foundation-runbook.md`
- `backend/src/main/resources/application*.yml`
- `backend/src/main/java/com/youyu/backend/controller/HealthController.java`
- `backend/src/main/java/com/youyu/backend/filter/RequestTraceFilter.java`
- `backend/src/main/java/com/youyu/backend/controller/admin/`
- `frontend/src/views/admin/DashboardView.vue`

## Allowed Changes

- Backend actuator/metrics configuration and focused health/metrics tests.
- Logging/trace improvements that do not expose secrets.
- Monitoring, alerting, incident, and runbook docs.
- Optional smoke scripts or examples for metrics and alert validation.
- `CHANGELOG.md` and this task lifecycle record.

## Implementation Plan

1. Inventory currently exposed health, metrics, logs, and admin operational signals.
2. Enable or document the minimal metrics surface needed for launch operations.
3. Define P0/P1 alert thresholds, owner fields, and response procedures.
4. Validate that log and metric access does not expose secrets or private data.

## Risks

- Exposing detailed actuator data publicly can leak infrastructure details.
- Alert thresholds can be too noisy without staging baseline data.
- Business metrics may require aggregation paths that are outside this task.

## Test Plan

- Backend: `cd backend; .\mvnw.cmd test` if actuator/metrics code changes.
- Frontend: not expected unless admin dashboard changes.
- API validation: health/metrics endpoints with production-like exposure settings.
- Manual: verify log search examples, alert rule syntax, dashboard/runbook references, and notification-channel blockers.

## Acceptance Criteria

- [x] P0 operational alerts have thresholds and response steps; concrete people/channel owners remain environment-specific.
- [x] Health endpoints are reachable only at the intended exposure level for the foundation phase.
- [x] Logs can support traceId-based troubleshooting without intentionally logging request bodies or secrets.
- [x] Business-signal monitoring gaps are documented with deferred status.
- [x] Incident response and post-incident record expectations are documented.

## Documentation Updates Required

- [ ] `CHANGELOG.md`
- [ ] logging/health, runbook, and operations standards
- [ ] task status and archive move

## Completion Notes

- 2026-06-04 sync: health and trace logging foundation is complete for staging rehearsal.
- 2026-06-04 sync: `launch-readiness-monitoring-alerting.md` now defines P0/P1 alert thresholds, initial response steps, log search keys, incident severities, and an incident record template.
- 2026-06-04 remaining: real metrics/alerting stack, dashboard access boundary, alert delivery channel, and named operational owners are still external environment decisions.
