# Launch Readiness Monitoring And Alerting

## Purpose

This document defines the minimum operational monitoring and incident-response
standard for a staging or public-demo launch candidate. It does not provision a
hosted monitoring service, alert channel, or dashboard. Those remain external
environment choices.

## Current Signals

- `GET /api/health`: application reachability and database probe through the
  public API path.
- `GET /actuator/health`: limited Actuator health with aggregate status, `db`,
  and `diskSpace`.
- Backend logs: stdout/stderr with `traceId` in the staging console pattern.
- API responses: standard envelope includes `traceId`.
- Redis health: disabled by default; enable `YOUYU_REDIS_HEALTH_ENABLED=true`
  only when Redis is intentionally treated as a required dependency.

`/actuator/metrics` and `/actuator/prometheus` remain unexposed in the
foundation profile. Do not expose detailed Actuator data publicly without an
authenticated internal network boundary.

## P0 Alerts

| Alert | Signal | Threshold | Initial response |
| --- | --- | --- | --- |
| Public API unavailable | `/api/health` | 3 consecutive failures or 5xx for 2 minutes | Check frontend/Nginx/backend containers, then backend logs by latest `traceId`. |
| Backend unhealthy | `/actuator/health` | status not `UP` for 2 minutes | Check database connectivity, disk space, and recent deployment changes. |
| Database unavailable | Actuator `db` or `/api/health` database status | 2 consecutive unhealthy checks | Stop release, inspect MySQL container/host, credentials, connection pool, and restore plan. |
| Disk pressure | Actuator `diskSpace` | free space below 15% | Stop uploads/backups if needed, prune generated artifacts outside the repo, and expand storage. |
| Payment abnormality | payment callback/log evidence | duplicate callback storms, verification failure, or payment success without order transition | Freeze payment release path and inspect payment records by order/payment id. |
| Backup failure | backup job exit code/log | any scheduled backup failure | Re-run manually, preserve logs, and verify the latest restorable backup. |

## P1 Alerts

| Alert | Signal | Threshold | Initial response |
| --- | --- | --- | --- |
| Elevated API errors | logs or gateway metrics | 5xx rate above 1% for 5 minutes | Group by endpoint and `traceId`; rollback if tied to a release. |
| Latency regression | gateway/k6/runtime metrics | p95 above 500 ms for 10 minutes on critical reads | Check database load, Redis state, slow queries, and recent deploys. |
| Redis cache degraded | Redis health/logs when enabled | health not `UP` or repeated cache exceptions | Confirm MySQL fallback works; disable Redis health if cache-only rehearsal should degrade. |
| Support backlog | admin/support list or business metric | queue grows for 30 minutes without handling | Assign operator, check notification delivery, and record staffing gap. |
| Report backlog | admin report list | unresolved reports exceed agreed threshold | Assign reviewer and record moderation SLA breach. |

## Log Search Keys

Use the following identifiers when investigating an incident:

- `traceId` from response header, response body, or backend logs.
- `userId` for authentication, profile, consent, and support issues.
- `orderId` and payment record id for order/payment/refund issues.
- report id, support ticket id, or conversation id for governance/support issues.

Do not copy password reset codes, JWTs, SMTP credentials, payment private keys,
database passwords, or full request bodies into incident records.

## Severity

| Severity | Definition | Response expectation |
| --- | --- | --- |
| SEV0 | Data loss, public secret exposure, payment/order corruption, or complete outage | Stop release or rollback immediately; preserve evidence; require owner signoff before resume. |
| SEV1 | Core purchase/login/order path impaired for many users | Assign owner, mitigate within the same operating window, and document follow-up. |
| SEV2 | Non-core feature degraded with workaround | Track in active task or issue list and schedule a fix. |
| SEV3 | Cosmetic, documentation, or low-impact operational issue | Record and batch with routine maintenance. |

## Incident Record Template

```markdown
### Incident record

- Date/time:
- Severity:
- Owner:
- Environment:
- Detection source:
- User-visible impact:
- Affected endpoint or workflow:
- Key traceId/orderId/paymentId/userId:
- Immediate mitigation:
- Root cause:
- Follow-up tasks:
- Release/blocker decision:
```

## External Blockers

Before production launch, choose and configure:

- hosted metrics/logging stack or equivalent self-hosted service
- alert delivery channel and on-call owner
- retention period for logs and incident records
- private access boundary for operational dashboards
- backup-job alert integration

