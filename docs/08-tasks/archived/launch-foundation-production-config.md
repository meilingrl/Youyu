# Task: Launch Foundation Production Config

## Metadata

- ID: launch-foundation-production-config
- Status: completed
- Owner: worker-a
- Track: cross-cutting
- Depends on: launch-foundation-scope-and-environments
- Priority: high
- Planned date: 2026-05-30
- Completed date: 2026-05-30

## Objective

Add staging configuration and environment examples while preserving local development defaults.

## Scope

- add staging Spring profile config using DB and JWT environment variables
- add safe environment example files and documentation
- keep schema initialization through existing Spring Boot `schema.sql`
- state that seed is opt-in and mock payment blocks production use

## Out of Scope

- Dockerfiles, Compose, Nginx
- application Java code, schema changes, remote deploy

## Allowed Changes

- `backend/src/main/resources/application-staging.yml`
- root environment example files
- configuration-focused documentation under `docs/04-standards/`

## Acceptance Criteria

- [ ] Staging config accepts the locked environment variables.
- [ ] No real secret is committed.
- [ ] Local development behavior remains unchanged.

## Completion Notes

Added the staging profile and `.env.example`, preserved schema-only startup, and
added `.env` to `.gitignore`. Reviewed and verified during integration.
