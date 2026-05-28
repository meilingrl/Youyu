# Youyu

Youyu is a campus marketplace project extracted into its own standalone repository from an earlier course workspace.

It keeps the original product scope and engineering structure, while giving the project a cleaner home for continued development, documentation, and release management.

## What This Repository Contains

Youyu is a full-stack web application for a student-focused marketplace experience, including:

- a Vue 3 frontend for browsing, selling, ordering, reviewing, and profile management
- a Spring Boot backend for authentication, products, shops, orders, reviews, admin governance, and reporting
- structured project documentation for requirements, architecture, roadmap, API specs, and task execution
- database design and seed assets used during development

## Project Status

This repository is the current development baseline copied from the latest usable local project snapshot before the standalone repository was created.

Important context:

- the repository name is now `Youyu`
- internal docs, package names, and code paths are now aligned to `Youyu`
- historical migration notes remain under `migration-notes/` for reference only

## Tech Stack

### Frontend

- Vue 3
- Vite
- Pinia
- Vue Router
- Element Plus
- Vitest
- Playwright

### Backend

- Spring Boot 3.3
- Java 17
- JDBC with mapper-based data access
- JWT authentication
- MySQL for local runtime configuration
- H2 for test execution

## Repository Structure

- `frontend/`: Vue application
- `backend/`: Spring Boot application
- `database/`: database design and migration reference material
- `docs/`: structured product, engineering, roadmap, API, and task documentation
- `resources/`: brand assets and supporting reference files
- `scripts/`: helper scripts
- `tests/`: test records and supporting assets
- `migration-notes/`: notes copied from the old course repository during extraction

## Quick Start

### 1. Clone the repository

```bash
git clone https://github.com/meilingrl/Youyu.git
cd Youyu
```

### 2. Start the backend

From `backend/`:

```bash
$env:SPRING_PROFILES_ACTIVE="seed"
mvnw.cmd spring-boot:run
```

Use the seed profile the first time when you want demo users, demo products, and seeded transaction data.

For normal daily startup:

```bash
$env:SPRING_PROFILES_ACTIVE=""
mvnw.cmd spring-boot:run
```

### 3. Start the frontend

From `frontend/`:

```bash
npm ci
npm run dev
```

The frontend development server runs at `http://localhost:5173` and proxies API requests to the backend at `http://localhost:8080`.

## Build And Test

### Frontend

```bash
npm run test
npm run build
```

### Backend

```bash
mvnw.cmd test
```

### End-to-end

From `frontend/`:

```bash
npm run test:e2e
```

This requires the backend and frontend to already be running.

## Documentation Guide

Start here if you want the full project context:

1. `AGENTS.md`
2. `CLAUDE.md`
3. `docs/README.md`
4. `docs/04-standards/development-process.md`
5. `docs/05-roadmap/current/stage-roadmap.md`

Useful documentation areas:

- `docs/01-product/`: product positioning and business context
- `docs/02-requirements/`: functional and non-functional requirements
- `docs/03-architecture/`: system and frontend/backend architecture
- `docs/04-standards/`: development and repository rules
- `docs/05-roadmap/`: current progress and open questions
- `docs/06-http/`: executable HTTP request collections
- `docs/08-tasks/`: active, draft, and archived execution tasks
- `docs/09-api-spec/`: formal API specifications

## Development Conventions

- keep the frontend and backend split intact
- route frontend API access through `frontend/src/api/`
- keep shared frontend state in Pinia stores
- preserve the backend layering: controller -> service -> mapper -> entity
- keep backend responses aligned with the shared `ApiResponse` envelope
- update `CHANGELOG.md` for substantive project work

## Notes About The Migration

This repository was created as a clean continuation path from a larger course workspace.

Because of that:

- some historical notes still refer to `Youyu`
- `migration-notes/` contains reference files copied from the old repository root
- the old course repository is being kept separately as backup and is not part of this repository's active workflow

## Recommended Workflow

- do new work in short-lived topic branches
- keep commits focused on one purpose
- verify frontend and backend changes locally before merging
- treat documentation updates as part of the implementation, not as optional follow-up

## License And Use

This project originated as course development work and is currently maintained as a personal project repository. If you plan to reuse or publish it more broadly, review the embedded course materials and project assets first.
