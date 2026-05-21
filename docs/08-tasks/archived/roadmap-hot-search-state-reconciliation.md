# Task: Roadmap Hot Search State Reconciliation

## Metadata

- ID: roadmap-hot-search-state-reconciliation
- Status: archived
- Owner: Claude
- Track: cross-cutting
- Depends on: archived `hot-search-enhancement-p3` record and current roadmap docs
- Priority: high
- Planned date: 2026-05-20
- Completed date: 2026-05-20

## Objective

Reconcile the roadmap and open-questions documents with the actual completed hot-search P3 delivery so the current planning layer stops advertising already-finished work as pending.

## Background

The archived hot-search P3 task is complete, but the current roadmap still lists hot-search P3 as pending. That drift weakens every later planning decision built on the roadmap.

## Scope

- update roadmap/open-questions docs to reflect hot-search P3 completion
- tighten any dependent wording about the next main lane
- record the reconciliation in task/changelog artifacts

## Out of Scope

- further hot-search feature work
- search backend/frontend refactor
- unrelated roadmap reprioritization

## Files to Read

- `AGENTS.md`
- `CLAUDE.md`
- `docs/README.md`
- `docs/04-standards/development-process.md`
- `docs/05-roadmap/current/stage-roadmap.md`
- `docs/05-roadmap/current/feature-roadmap.md`
- `docs/05-roadmap/current/open-questions.md`
- `docs/08-tasks/archived/hot-search-enhancement-p3.md`
- `CHANGELOG.md`

## Allowed Changes

- `docs/05-roadmap/current/*.md`
- `docs/08-tasks/active/*.md`
- `CHANGELOG.md`

## Implementation Plan

1. Compare current roadmap wording with archived hot-search P3 completion evidence.
2. Update roadmap state and any dependent open-question wording that is now stale.
3. Record the reconciliation and close the task.

## Risks

- over-editing roadmap language beyond the hot-search state correction
- accidentally removing still-relevant search follow-up notes

## Test Plan

- Backend: not required
- Frontend: not required
- API validation: not required
- Manual:
  - confirm roadmap no longer marks hot-search P3 as pending
  - confirm current next-step ordering still reads coherently

## Acceptance Criteria

- [x] Current roadmap no longer contradicts the archived hot-search P3 delivery
- [x] Any related open-question wording is updated if it depended on P3 still being pending
- [x] `CHANGELOG.md` records the docs reconciliation

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [x] relevant files in `docs/06-http/`
- [x] roadmap or standards docs if applicable
- [x] task status and archive move

## Completion Notes

### Changes
- `stage-roadmap.md`: Removed "完成热搜 P3" from P1, elevated chat MVP task spec + platform mediation boundary to P1, redistributed P2/P3 around actual active work
- `stage-roadmap.md`: Replaced "热搜 P3 完成或明确延期" entry condition with "聊天 MVP 与平台调解边界已通过正式任务规格明确"
- `feature-roadmap.md`: Removed hot-search P3 from the "当前功能状态" table, moved it to "已完成但不再作为当前主线展开的能力"
- `feature-roadmap.md`: Updated "正式 API 规范化" status from "未开始" to "进行中" (auth/product/order/admin/user/search already spec'd)
- `feature-roadmap.md`: Removed hot-search P3 from "推荐推进顺序" and "依赖关系"
- `open-questions.md`: Removed "## 3. 热搜 P3 的目标界定" (resolved by the delivered P3 scope: prefix suggestion + governance filtering + aggregation optimization)

### Verification
- stage-roadmap.md no longer marks hot-search P3 as pending
- feature-roadmap.md status table no longer lists hot-search P3
- open-questions.md no longer contains unresolved hot-search P3 scope questions
- Next-step ordering (chat MVP → platform mediation → bundle governance → API spec) is coherent with active task docs
- No product code, schema, or endpoint changes
