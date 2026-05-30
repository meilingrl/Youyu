# Launch Foundation Performance Baseline

## Purpose

The launch-foundation performance baseline is a reproducible reachability smoke for the staging rehearsal environment. It exercises four stable public read endpoints without introducing application changes or claiming production capacity.

The executable script and report template live in `scripts/performance/`.

## Required Endpoints

| Endpoint | Purpose |
| --- | --- |
| `/api/health` | Backend reachability |
| `/api/products?page=1&pageSize=12` | Public product list read |
| `/api/search/hot` | Public hot-search read |
| `/api/recommend/home?limit=8` | Public recommendation read |

Each request must return HTTP `200` and the standard API envelope field `"success": true`.

## Execution Standard

Use Docker as the default execution path so a workstation does not need a native k6 installation:

```powershell
docker run --rm -i `
  -e BASE_URL=http://host.docker.internal:18080 `
  -v "${PWD}/scripts/performance:/scripts" `
  grafana/k6 run /scripts/smoke.js
```

Use `BASE_URL=http://host.docker.internal:18080` when validating the frontend
Nginx same-origin `/api` proxy. Native k6 remains supported when available.

The default smoke load is intentionally lightweight: `2` virtual users for `10s`, with a `0.2s` pause between iterations. `VUS`, `DURATION`, and `SLEEP_SECONDS` may be overridden for a rehearsal run, but overrides do not turn this smoke into capacity acceptance.

## Evidence

For each recorded run:

1. Copy `scripts/performance/REPORT_TEMPLATE.md`.
2. Record the commit SHA, environment, base URL, seed state, command, and k6 version.
3. Preserve the k6 summary outside `scripts/performance/`, such as under `.artifacts/performance/`.
4. Record endpoint checks, failed-request rate, p95 duration, and observations.

## Deferred Capacity Acceptance

This launch-foundation smoke does not satisfy the roadmap target of `500` concurrent users, `p95 < 500ms`, and error rate `< 1%`.

That acceptance remains deferred until a dedicated staging load-test window is available with representative seed data, recorded host resources, database configuration, resource-utilization evidence, and preserved test outputs.
