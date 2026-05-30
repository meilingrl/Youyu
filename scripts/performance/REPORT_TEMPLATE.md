# Youyu Performance Baseline Report

## Run Metadata

| Field | Value |
| --- | --- |
| Date and time | |
| Operator | |
| Commit SHA | |
| Environment | local / staging rehearsal |
| Base URL | |
| Seed data enabled | yes / no |
| k6 version | |
| Command | |
| Summary export path | |

## Environment Notes

- Host resources:
- Backend configuration:
- Database configuration:
- Dataset notes:
- Known environmental constraints:

## Smoke Evidence

| Endpoint | Checks passed | Request count | Notes |
| --- | --- | --- | --- |
| `/api/health` | | | |
| `/api/products?page=1&pageSize=12` | | | |
| `/api/search/hot` | | | |
| `/api/recommend/home?limit=8` | | | |

| k6 Metric | Result |
| --- | --- |
| `checks` rate | |
| `http_req_failed` rate | |
| `http_req_duration` p(95) | |
| Total requests | |

## Observations

-

## Deferred Capacity Acceptance

This report records a lightweight smoke baseline only. It does not satisfy or claim the deferred roadmap acceptance target of `500` concurrent users, `p95 < 500ms`, and error rate `< 1%`.

Before capacity acceptance, run a separate staging load test with representative data, record resource utilization, and preserve the full test configuration and outputs.
