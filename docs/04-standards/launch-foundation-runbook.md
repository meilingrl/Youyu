# Launch Foundation Staging Rehearsal Runbook

This runbook covers a single-host staging rehearsal. It proves that the current
application can be built, started, checked, backed up, and restored. It is not
a production deployment procedure.

## Production Blockers

- The current payment integration is a mock implementation. It must be replaced
  and reviewed before any real production release.
- Redis, rate limiting, OSS, Flyway, remote deployment, full monitoring,
  privacy compliance work, security final acceptance, and L7 release acceptance
  remain deferred.
- HTTP is the default rehearsal entry point. A real release must install and
  verify HTTPS using the provided Nginx template and an environment-specific
  certificate mount.

## Environment

Copy `.env.example` to an untracked `.env`, then replace every placeholder with
a generated value. The required application and database variables are
`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`,
`MYSQL_ROOT_PASSWORD`, and `APP_JWT_SECRET`.

Optional host-port overrides are `HTTP_PORT` and `BACKEND_PORT`. Their defaults
are `80` and `8080`.

## Start And Check

Start the schema-only staging rehearsal:

```bash
docker compose up -d --build
docker compose ps
curl http://localhost/api/health
curl http://localhost:8080/actuator/health
```

The first URL verifies frontend to Nginx to backend routing. The second verifies
the minimal Actuator health endpoint, including database health. Only health is
exposed; metrics and Prometheus endpoints are intentionally unavailable.

Start the explicit demo overlay only when stable seed records are needed:

```bash
docker compose -f compose.yml -f compose.demo.yml up -d --build
```

The default command activates `staging`; the demo overlay activates
`staging,seed`. Spring Boot continues to initialize the existing `schema.sql`;
this wave does not introduce Flyway.

## Logs

```bash
docker compose logs -f backend
```

Backend logs stay on the console. Request logs include an MDC `traceId`, which
is cleared when each request ends.

## Backup And Restore

On a host with `bash`, `mysqldump`, `mysql`, and `gzip`:

```bash
MYSQL_ROOT_PASSWORD=<root-password> bash scripts/backup-mysql.sh
MYSQL_ROOT_PASSWORD=<root-password> bash scripts/restore-mysql.sh \
  --backup-file backups/mysql/youyu_<timestamp>.sql.gz \
  --target-db youyu_restore_rehearsal
```

Backups are gzip-compressed and retained for 7 days by default. Restore targets
must start with `youyu_restore_`, must not exist yet, and cannot be `youyu`.

## Scanning

```bash
docker run --rm -v "$PWD:/repo" zricethezav/gitleaks:latest \
  detect --source=/repo --no-git --redact --exit-code 1
cd frontend && npm audit --package-lock-only --json
cd ../backend && ./mvnw org.owasp:dependency-check-maven:12.2.2:check \
  -B -DfailBuildOnCVSS=11
```

Gitleaks blocks secret findings. Dependency scans are report-only during this
foundation wave. OWASP Dependency-Check may take a long time on its first
vulnerability-data sync.

## Performance Smoke

```bash
docker run --rm -e BASE_URL=http://host.docker.internal \
  -v "$PWD:/work" -w /work grafana/k6:latest \
  run scripts/performance/smoke.js
```

This is a baseline smoke for `/api/health`, `/api/products`,
`/api/search/hot`, and `/api/recommend/home`. It is not a 500-concurrency
capacity acceptance test.

## Stop And Roll Back

```bash
docker compose down
docker compose down -v
```

The second command also removes the local rehearsal database volume. Use it
only when intentionally resetting rehearsal data.

## 2026-05-30 Integration Evidence

- Backend tests: 162 passed.
- Frontend tests: 39 passed; frontend production build completed.
- Default Compose: schema only, 0 users and 0 products.
- Demo overlay: 14 users and 9 products; stable demo user `zhangsan`.
- Nginx proxy `/api/health`: HTTP 200; frontend root: HTTP 200.
- `/actuator/health`: `UP` with `db`, `diskSpace`, and `ping` components.
- MySQL outage exercise: Actuator did not report `UP`, but the JDBC probe did
  not return within 20 seconds. Health timeout tuning remains a follow-up.
- Backup restore: gzip backup created, temporary restore preserved 14 users and
  9 products, and restore to `youyu` was rejected.
- Gitleaks: passed after narrowly allowing two known JWT placeholder examples.
- `npm audit`: 0 vulnerabilities.
- OWASP Dependency-Check: local first sync did not finish within 10 minutes;
  CI remains configured to generate its report without blocking this wave.
- k6 smoke: 368 requests, 0 failures, p95 19.8 ms with 2 VUs for 10 seconds.
