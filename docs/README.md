# Youyu Documentation

This directory is the single structured documentation workspace for `Youyu`.

The goal of this system is not to collect notes indefinitely. It is to keep product intent, engineering rules, roadmaps, API contracts, and executable tasks separated clearly enough that both humans and AI agents can work without guessing.

## Directory Map

### `01-product/`

Human-facing product documents in Chinese:

- product positioning
- business scenarios
- user and role model
- catalog and governance concepts

### `02-requirements/`

Formal requirement documents in Chinese:

- functional requirements
- non-functional requirements
- core domain model
- order lifecycle
- analytics and preference requirements

### `03-architecture/`

Engineering architecture documents:

- tech stack and architecture principles
- repository structure
- payment implementation strategy
- UI/UX constitution and redesign direction
- frontend information architecture and route distribution
- data management and privacy compliance
- performance and scalability guide

### `04-standards/`

Repository standards and governance:

- development process
- frontend redesign safety
- document system
- contribution workflow
- testing workflow
- course constraints and acceptance rules
- operations and deployment standards
- glossary

### `05-roadmap/`

Planning documents with distinct responsibilities:

- `current/stage-roadmap.md`
  - current phase and stage progression
- `current/feature-roadmap.md`
  - feature expansion order and status
- `current/open-questions.md`
  - unresolved items that are not yet formal decisions
- `archived/`
  - historical roadmap and MVP planning artifacts

### `06-http/`

Executable HTTP request collections used for validation, smoke checks, and manual integration checks.

### `07-decisions/`

Architecture Decision Records (ADRs). Historical decisions stay here permanently.

### `08-tasks/`

Task execution documents:

- `drafts/`
- `active/`
- `archived/`
- `TASK_TEMPLATE.md`
- `README.md`

### `09-api-spec/`

Formal API specification documents.

- contract overviews
- field definitions
- error-code documentation
- current module specs:
  - `admin.md`
  - `auth.md`
  - `cart.md`
  - `marketing.md`
  - `order.md`
  - `payment.md`
  - `product.md`
  - `recommend.md`
  - `report.md`
  - `review.md`
  - `search.md`
  - `shop.md`
  - `support.md`
  - `user.md`

## Reading Order

### For new contributors or AI agents

1. `../AGENTS.md`
2. `../CLAUDE.md`
3. `README.md`
4. `04-standards/development-process.md`
5. `05-roadmap/current/stage-roadmap.md`
6. `05-roadmap/current/feature-roadmap.md`
7. Relevant module docs and task docs

### For product/background understanding

1. `01-product/`
2. `02-requirements/`
3. `05-roadmap/`

### For implementation work

1. `03-architecture/`
2. `04-standards/`
3. `06-http/`
4. `09-api-spec/` when a task depends on formal API contracts
5. `08-tasks/active/`

## Update Rules

- Update only the document class that owns the fact you are changing.
- Do not place feature execution detail into roadmap documents.
- Do not place long-term repository rules into task documents.
- Do not delete completed task history; move it to `08-tasks/archived/`.
- When code changes alter endpoints, update the matching file in `06-http/`.

For detailed maintenance rules, see `04-standards/document-system.md`.
