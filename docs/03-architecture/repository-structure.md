# Repository Structure

## Top-Level Layout

- `docs/`
  - structured project documentation
- `frontend/`
  - Vue 3 application
- `backend/`
  - Spring Boot application
- `database/`
  - database design assets and migration references
- `resources/`
  - supporting reference assets
- `scripts/`
  - helper scripts
- `tests/`
  - test assets and supporting records

## Docs Layout

The `docs/` tree is organized by responsibility:

- `01-product/`
- `02-requirements/`
- `03-architecture/`
- `04-standards/`
- `05-roadmap/`
- `06-http/`
- `07-decisions/`
- `08-tasks/`
- `09-api-spec/`

See `../README.md` for the document map.

## Frontend Layout

Recommended working structure:

- `src/views`
- `src/components`
- `src/stores`
- `src/router`
- `src/api`
- `src/utils`

## Backend Layout

Recommended layered structure:

- `controller`
- `service`
- `mapper`
- `entity`
- `config`
- `filter`
- `listener`

## Repository Rules

- New formal code stays inside `CampusMarket/`.
- Documents and code should stay aligned by module and responsibility.
- Documentation restructuring should preserve discoverability and avoid duplicate ownership.
