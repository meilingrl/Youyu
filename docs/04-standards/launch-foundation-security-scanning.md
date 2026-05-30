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

## Follow-Up Boundary

- Triage and remediation of findings belong to separate hardening tasks.
- Container image scanning is deferred.
- The staging foundation does not claim final security acceptance.
