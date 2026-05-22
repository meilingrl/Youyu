# API Spec: search

## Document Info

- Status: active
- Source of truth:
  - controllers:
    - `backend/src/main/java/com/youyu/backend/controller/search/SearchController.java`
    - `backend/src/main/java/com/youyu/backend/controller/admin/AdminController.java`
  - request sample: `docs/06-http/search.http`
- Last updated: 2026-05-16

## Scope

This document covers:

- public hot-search ranking
- public search suggestions
- admin search-governance rule management
- admin search-log browsing

It does not cover product list filtering itself. Product listing remains specified in `product.md`.

## Authentication

- Public:
  - `GET /api/search/hot`
  - `GET /api/search/suggest`
- Admin only:
  - `GET /api/admin/search/governance-rules`
  - `POST /api/admin/search/governance-rules`
  - `PUT /api/admin/search/governance-rules/{ruleId}`
  - `DELETE /api/admin/search/governance-rules/{ruleId}`
  - `GET /api/admin/search/logs`

## Response Envelope

All endpoints in this module use the unified response envelope with `success`, `code`, `message`, `data`, and `traceId`.

## Endpoints

### `GET /api/search/hot`

#### Purpose

Return the public hot-search ranking based on recent search-log activity.

#### Response

- `data`: array of ranking items
- Current item fields:
  - `keyword`
  - `normalizedKeyword`
  - `searchCount`
  - `resultCountSum`
  - `score`
  - `pinned`

#### Notes

- Ranking currently uses a 7-day window with decay weighting.
- Governance rules may hide, block, or pin keywords before the final top-10 list is returned.
- `resultCountSum` currently reflects the accumulated `result_count` values recorded at search time, which are based on the returned page size from product search.

### `GET /api/search/suggest`

#### Purpose

Return prefix-based search suggestions derived from recent `search_logs`.

#### Request

- Query:
  - `q`: optional raw input string
  - `limit`: optional, default `8`, effective range currently `1..8`

#### Response

- `data`: array of suggestion items
- Current item fields:
  - `keyword`
  - `normalizedKeyword`
  - `searchCount`
  - `resultCountSum`
  - `score`
  - `pinned`

#### Notes

- Suggestions are generated only from existing search-log history in the current implementation.
- Blank, whitespace-only, or punctuation-only input returns an empty list.
- Suggestion requests do not create new search-log records.
- Governance filtering reuses the same hidden / blocked semantics as hot ranking.
- `PIN_KEYWORD` can move an existing prefix-matching suggestion to the front, but does not synthesize unrelated suggestions for the current prefix.

### `GET /api/admin/search/governance-rules`

#### Purpose

List all governance rules that affect hot-search and suggestion behavior.

#### Response

- `data`: array of governance-rule objects
- Current fields include:
  - `id`
  - `ruleType`
  - `keyword`
  - `displayLabel`
  - `isActive`
  - `createdAt`
  - `updatedAt`

### `POST /api/admin/search/governance-rules`

#### Purpose

Create a new search-governance rule.

#### Request

- Body:
  - `ruleType`: required
  - `keyword`: required
  - `displayLabel`: optional, currently relevant to pin rules

#### Supported `ruleType`

- `SENSITIVE_WORD`
- `STOP_WORD`
- `HIDE_KEYWORD`
- `PIN_KEYWORD`

#### Error Cases

- `400`: invalid rule type or invalid keyword
- business error: duplicate rule conflicts with the `(rule_type, keyword)` uniqueness constraint

### `PUT /api/admin/search/governance-rules/{ruleId}`

#### Purpose

Update an existing search-governance rule.

#### Request

- Body currently supports partial updates for:
  - `keyword`
  - `displayLabel`
  - `isActive`

#### Error Cases

- `404`: rule does not exist
- `400`: updated keyword is invalid

### `DELETE /api/admin/search/governance-rules/{ruleId}`

#### Purpose

Delete a governance rule.

#### Response

- `data.deleted`: boolean success marker

#### Error Cases

- `404`: rule does not exist

### `GET /api/admin/search/logs`

#### Purpose

Browse stored search logs in reverse chronological order.

#### Request

- Query:
  - `page`: optional, default `1`
  - `pageSize`: optional, default `10`, effective maximum `50`

#### Response

- `data.items`: paginated search-log rows
- `data.total`
- `data.page`
- `data.pageSize`

#### Current Search-Log Row Fields

- `id`
- `keyword`
- `normalizedKeyword`
- `userId`
- `resultCount`
- `createdAt`

## Governance Semantics

- `HIDE_KEYWORD`
  - Exact-match hide by normalized keyword.
- `SENSITIVE_WORD`
  - Block exact-match and substring-match keywords from public ranking and suggestions.
- `STOP_WORD`
  - Same current public-filter semantics as `SENSITIVE_WORD`.
- `PIN_KEYWORD`
  - For hot ranking:
    - promote an existing keyword to the front, or synthesize a pinned fallback item if the keyword is absent
  - For suggestions:
    - only reorder an existing prefix-matching suggestion; no unrelated fallback suggestion is created

## Notes

- Search-domain payloads are currently map-based rather than strict DTO-based.
- If the project later formalizes search DTOs or OpenAPI exports, this spec should be updated to reflect the new contract source.
