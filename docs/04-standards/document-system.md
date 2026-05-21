# Document System

This document defines the ownership boundaries of the `docs/` tree.

The main principle is simple: one document type owns one kind of truth.

## 1. Document Classes

### Long-lived documents

These describe current facts or stable rules and are expected to stay useful across many iterations:

- `01-product/`
- `02-requirements/`
- `03-architecture/`
- `04-standards/`
- `06-http/`
- `09-api-spec/`
- `07-decisions/`

### Planning documents

These describe what the team intends to do next, but not the exact step-by-step implementation details:

- `05-roadmap/current/stage-roadmap.md`
- `05-roadmap/current/feature-roadmap.md`
- `05-roadmap/current/open-questions.md`
- `05-roadmap/archived/*`

### Task execution documents

These describe one concrete implementation item and its execution history:

- `08-tasks/drafts/`
- `08-tasks/active/`
- `08-tasks/archived/`

### Change history

These record what already happened:

- `../CHANGELOG.md`
- `07-decisions/*.md`
- `08-tasks/archived/*.md`

## 2. What Goes Where

Use the following ownership model:

- Product meaning, audience, and business goals
  - `01-product/`
- Formal requirements and acceptance boundaries
  - `02-requirements/`
- Engineering structure and implementation strategy
  - `03-architecture/`
- Process, workflow, testing, contribution, terminology
  - `04-standards/`
- Stage and feature sequencing
  - `05-roadmap/`
- Executable HTTP validation
  - `06-http/`
- Formal API specifications
  - `09-api-spec/`
- Significant architecture decisions
  - `07-decisions/`
- Concrete, bounded execution work
  - `08-tasks/`

## 3. Task Lifecycle Rules

Task documents follow a strict lifecycle:

- `drafts/`
  - possible future work or not-yet-approved work
- `active/`
  - approved work with current execution value
- `archived/`
  - completed or superseded work retained for traceability

Rules:

- Do not delete task history after completion.
- Do not keep completed tasks in `active/`.
- Do not keep planning-only ideas in `active/`.
- If work spans multiple modules without fitting one feature, mark the task `Track: cross-cutting`.

## 4. Writing Rules

- Use English filenames in kebab-case.
- Use document language based on audience:
  - AI / engineering execution docs: English
  - human-facing product / requirement / decision docs: Chinese
- One document should own one primary topic.
- If two documents repeat the same rule, consolidate them.

## 5. Migration and Cleanup Rules

- When restructuring documents, migrate content into the owning document class.
- Do not leave duplicate “old” and “new” versions active in parallel.
- Use Git history for ordinary textual history.
- Use archived task docs for execution history.

## 6. Update Matrix

Update the correct document when:

- process changes -> `04-standards/`
- stage or feature status changes -> `05-roadmap/`
- endpoint changes -> `06-http/`
- major technical decision changes -> new ADR in `07-decisions/`
- concrete work starts or ends -> `08-tasks/`
