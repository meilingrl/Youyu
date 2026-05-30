# Task: Launch Foundation Performance Baseline

## Metadata

- ID: launch-foundation-performance-baseline
- Status: completed
- Owner: worker-e
- Track: cross-cutting
- Depends on: launch-foundation-scope-and-environments
- Priority: medium
- Planned date: 2026-05-30
- Completed date: 2026-05-30

## Objective

Add a reproducible k6 smoke and report template without attempting performance optimization.

## Scope

- k6 scenario for health, product list, hot search and home recommendations
- configurable base URL and lightweight default smoke load
- report template separating baseline evidence from future target acceptance

## Out of Scope

- Redis, indexes, rate limiting, 500-user acceptance claim

## Allowed Changes

- `scripts/performance/`
- performance-baseline documentation

## Acceptance Criteria

- [ ] k6 script covers the four locked endpoints.
- [ ] Defaults are suitable for a quick smoke.
- [ ] Documentation states that 500-user acceptance remains deferred.

## Completion Notes

Added k6 baseline smoke scripts and templates. Live Docker smoke completed with
368 requests, zero failures, and p95 19.8 ms at 2 VUs for 10 seconds.
