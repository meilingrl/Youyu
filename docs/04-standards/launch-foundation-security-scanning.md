# Launch Foundation Security Scanning

This document describes the lightweight Launch Foundation security scans. These
checks improve visibility before staging rehearsals. They are not a replacement
for dependency remediation, penetration testing, or final security acceptance.

## CI Workflow

`.github/workflows/security-scanning.yml` runs on pushes and pull requests to
`master`, and can also be started manually.

| Check | Purpose | Launch Foundation behavior |
| --- | --- | --- |
| Gitleaks | Detect committed secrets | Blocking |
| OWASP Dependency-Check | Report known backend dependency vulnerabilities | Report only |
| `npm audit` | Report known frontend dependency vulnerabilities | Report only |

Dependency reports are uploaded as GitHub Actions artifacts when generated.
The repository `.gitleaks.toml` extends the default rules and narrowly allows
two known JWT placeholder examples used by `.env.example` and an archived task
record. It does not suppress real credentials or broad classes of findings.
The repository `.gitleaksignore` also records the exact historical fingerprint
for the committed `.env.example` placeholder so git-history scans remain
blocking for any new secret-like value.
OWASP Dependency-Check may take longer on its first run because it downloads
vulnerability data. Its report step remains non-blocking during this wave so
network or upstream data-feed issues do not hide the blocking secret scan.

## Reproduce Locally

Run OWASP Dependency-Check from the repository root:

```bash
cd backend
./mvnw org.owasp:dependency-check-maven:12.2.2:check \
  -B \
  -DfailBuildOnCVSS=11 \
  -Dformat=HTML \
  -Dodc.outputDirectory=../reports/backend-dependency-check
```

On Windows PowerShell, use `.\mvnw.cmd` instead of `./mvnw`.

Run the frontend dependency report:

```bash
cd frontend
npm audit --package-lock-only --json > ../reports/npm-audit.json
npm audit --package-lock-only
```

Install Gitleaks from its official release packages, then run from the repository
root:

```bash
gitleaks detect --source . --verbose
```

The equivalent Docker command is:

```bash
docker run --rm -v "$PWD:/repo" zricethezav/gitleaks:latest \
  detect --source=/repo --no-git --redact --exit-code 1
```

The dependency commands may exit non-zero when findings exist. CI intentionally
records those findings without blocking this foundation wave. Local report
files under `reports/` are generated output and should not be committed.

## Launch Hardening Runtime Checks

Run backend security boundary tests from `backend/`:

```powershell
.\mvnw.cmd test "-Dtest=CorsPropertiesTest,MockAuthBoundaryTest,MockPaymentExposureGuardTest,MockPaymentExposureTest,JwtSecretGuardTest"
```

Run the full backend regression suite after the working tree compiles:

```powershell
.\mvnw.cmd test
```

Production-like profiles, including `staging`, must provide explicit CORS
origins and a non-default JWT secret. Wildcard CORS origins are forbidden under
production-like profiles. Local `dev`, `seed`, `test`, and unset/default
profiles keep the Vite/backend local origins so local development remains
usable.

Mock authentication is limited to local-safe profiles. `Bearer ***{userId}-{role}`
and `X-User-Id` / `X-User-Role` header auth must not authenticate requests in
`staging` or production-like profiles. Local HTTP collections may still use mock
tokens for dev/seed/test smoke checks.

Mock payment is a local/testing gateway only. `staging` disables
`youyu.payment.mock-enabled` by default and production-like profiles fail fast if
mock payment remains enabled. Alipay sandbox remains a rehearsal mechanism, not
production payment approval.

Use `docs/06-http/security-hardening.http` for manual API evidence covering CORS
preflight behavior, auth-protected endpoints, admin permission regression, and
payment gateway exposure.

## SQL And Sensitive-Data Review Evidence

Mapper review focused on dynamic SQL and sort handling. Current evidence:

- Product search sort is allowlisted in `JdbcProductMapper` (`price_asc`,
  `price_desc`, `sales_desc`, `newest`) and falls back to a fixed newest sort.
- Search, admin list, report, support, mediation, user, shop, and order mapper
  queries use fixed `ORDER BY` clauses with positional or named parameters for
  user-controlled values.
- No reviewed mapper path directly interpolates user-provided sort fields into
  SQL. Remaining future mapper additions must preserve allowlisted sort clauses
  instead of accepting raw column names or directions.

Sensitive response/log review focused on security-owned surfaces:

- JWT secrets remain environment-injected for production-like profiles and are
  not returned by auth endpoints.
- Mock-token and payment boundaries are configuration-gated and covered by
  tests.
- Unhandled exception logging still records stack traces for server-side
  diagnosis; operators must treat logs as sensitive and avoid logging request
  bodies, JWTs, SMTP credentials, payment keys, verification codes, certificates,
  generated reports, backups, or local `.env` values.

## Follow-Up Boundary

- Triage and remediation of findings belong to separate hardening tasks.
- Container image scanning is deferred.
- HTTPS certificate provisioning and real gateway termination are external
  production blockers and are not completed by repository code changes.
- Payment provider productionization remains out of scope; sandbox/mock evidence
  does not approve real production payment collection.
- The staging foundation does not claim final security acceptance.
