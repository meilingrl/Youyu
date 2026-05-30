# Performance Smoke Baseline

This directory contains the lightweight k6 smoke for launch-foundation staging rehearsal. It verifies that four public read endpoints remain reachable through the configured base URL. It is a reproducible baseline tool, not a performance optimization or launch-capacity claim.

## Prerequisites

1. Install Docker. A native k6 installation is optional.
2. Start the backend directly or start the Compose staging rehearsal environment.
3. Use a seed-enabled environment when you want representative product and recommendation data. The default schema-only staging mode is still valid for reachability smoke checks.

## Run With Docker

From the repository root, run the image directly. `host.docker.internal` lets the k6 container reach a backend or Nginx proxy published on the host:

```bash
docker run --rm -i \
  -e BASE_URL=http://host.docker.internal:18080 \
  -v "$PWD/scripts/performance:/scripts" \
  grafana/k6 run /scripts/smoke.js
```

PowerShell:

```powershell
docker run --rm -i `
  -e BASE_URL=http://host.docker.internal:18080 `
  -v "${PWD}/scripts/performance:/scripts" `
  grafana/k6 run /scripts/smoke.js
```

For Linux hosts where `host.docker.internal` is not predefined, add:

```bash
--add-host host.docker.internal:host-gateway
```

The smoke uses `2` virtual users for `10s` and pauses `0.2s` between iterations. Each iteration requests:

- `/api/health`
- `/api/products?page=1&pageSize=12`
- `/api/search/hot`
- `/api/recommend/home?limit=8`

Every response must return HTTP `200` with the standard API envelope field `"success": true`.

## Run With Native k6

If [k6](https://grafana.com/docs/k6/latest/set-up/install-k6/) is installed, the default script URL is `http://localhost:18080`:

```bash
k6 run scripts/performance/smoke.js
```

## Configure A Rehearsal Run

Set `BASE_URL` when the backend is available through another origin, such as the frontend Nginx proxy:

```bash
BASE_URL=http://localhost:18080 k6 run scripts/performance/smoke.js
```

PowerShell:

```powershell
$env:BASE_URL = 'http://localhost:18080'
k6 run scripts/performance/smoke.js
```

Optional smoke controls:

| Variable | Default | Purpose |
| --- | --- | --- |
| `BASE_URL` | `http://localhost:18080` | Backend or Nginx origin without a trailing `/` requirement |
| `VUS` | `2` | Lightweight smoke virtual users |
| `DURATION` | `10s` | Lightweight smoke duration |
| `SLEEP_SECONDS` | `0.2` | Delay between iterations |

To preserve a machine-readable summary:

```bash
k6 run --summary-export performance-smoke-summary.json scripts/performance/smoke.js
```

With Docker, mount an output directory and write the summary there:

```powershell
New-Item -ItemType Directory -Force .artifacts/performance | Out-Null
docker run --rm -i `
  -e BASE_URL=http://host.docker.internal:18080 `
  -v "${PWD}/scripts/performance:/scripts" `
  -v "${PWD}/.artifacts/performance:/results" `
  grafana/k6 run --summary-export /results/performance-smoke-summary.json /scripts/smoke.js
```

Attach the command, environment details, k6 output, and summary path to a copy of [REPORT_TEMPLATE.md](REPORT_TEMPLATE.md). Keep generated summaries outside `scripts/performance/`.

## Deferred Acceptance

This smoke does **not** prove launch capacity. The roadmap acceptance target of `500` concurrent users, `p95 < 500ms`, and error rate `< 1%` remains deferred until a staging environment, representative data set, and dedicated load-test window are available. Do not report this smoke as satisfying that target.
