# ADR: Product Search Keyword Strategy

- Date: 2026-05-22
- Status: accepted
- Task: product-search-path-hardening (Slice D of architecture-performance-hardening)

## Context

The public product listing path (`GET /api/products`) is served by `ProductServiceImpl.listProducts`, which delegates to `JdbcProductMapper.findPublicByFiltersPaged` and `countPublicByFilters`. These methods build a dynamic SQL query against the `products` table with the following structure:

```sql
-- Fixed predicates (always present)
WHERE p.is_deleted = FALSE
  AND p.status = 'on_sale'
  AND (p.review_status = 'not_required' OR p.review_status = 'approved')

-- Optional predicates (bound to caller-supplied parameters)
AND p.category_id = ?          -- when categoryId is provided
AND p.product_type = ?         -- when productType is provided

-- Keyword scan (when keyword is provided)
AND (
    LOWER(COALESCE(p.title, ''))       LIKE ?   -- e.g. '%notebook%'
 OR LOWER(COALESCE(p.subtitle, ''))   LIKE ?
 OR LOWER(COALESCE(p.description, '')) LIKE ?
 OR LOWER(COALESCE(c.name, ''))        LIKE ?   -- joined category name
)

ORDER BY p.created_at DESC
LIMIT ? OFFSET ?
```

The keyword matching uses a leading-wildcard `LIKE '%kw%'` pattern. This pattern is **index-skipping by nature**: no B-tree index on a single text column can satisfy a leading-wildcard scan. The goal of this ADR is therefore not to make keyword matching index-backed, but to make the *non-keyword filter combination* fast enough that the residual keyword scan operates on a minimally-sized candidate set.

Prior to this task, the only indexes on `products` were single-column indexes:

| Index | Columns |
|-------|---------|
| `idx_products_seller` | `(seller_user_id)` |
| `idx_products_shop` | `(shop_id)` |
| `idx_products_status` | `(status)` |
| `idx_products_review_status` | `(review_status)` |
| `idx_products_type` | `(product_type)` |

None of these covers the composite filter combination used by the public listing path, nor includes `created_at` for the ORDER BY clause.

## Decision

Keep the current `LOWER(...) LIKE '%kw%'` substring matching as-is. Add two composite indexes that cover the most common public-listing filter combinations so the engine can seek to a small row set before applying the keyword scan.

### Indexes added

```sql
-- Covers the always-present base filters + ORDER BY column.
-- Engine can seek to (status='on_sale', is_deleted=FALSE) and scan created_at DESC.
CREATE INDEX idx_products_public_base
    ON products(status, is_deleted, created_at);

-- Covers the filtered path when product_type and/or category_id are supplied.
-- Column order: high-selectivity fixed filters first, optional filters next, sort column last.
CREATE INDEX idx_products_public_type_cat
    ON products(status, is_deleted, product_type, category_id, created_at);
```

### Column-order rationale

- `status` first — always bound to `'on_sale'`; single-value equality maximally narrows the scan.
- `is_deleted` second — always bound to `FALSE`; second equality further narrows.
- `product_type` / `category_id` third/fourth — optional equality filters when present; skipped by the optimizer when absent.
- `created_at` last — ORDER BY column; when all equality predicates are satisfied the engine can read rows in index order, avoiding a file-sort.

`review_status` is not included in the composite because it appears in an OR condition (`NOT_REQUIRED OR approved`). An equality column in an OR reduces to a range scan at best; placing it in the composite would only add index width without enabling a seek. The engine can apply it cheaply as a post-filter against the already-narrow result set produced by the other predicates.

## Considered Alternatives

### MySQL FULLTEXT index

A `FULLTEXT` index on `(title, subtitle, description)` with `MATCH ... AGAINST (? IN BOOLEAN MODE)` would make keyword matching index-backed and support natural-language ranking.

**Rejected for now because:**
- Requires MySQL 5.6+. The current test profile uses H2 in-memory DB, which does not support `FULLTEXT` syntax. Adding a `FULLTEXT` index to `schema.sql` would break the H2 test suite unless a separate migration strategy is introduced.
- `FULLTEXT` does not support multi-column cross-table keyword matching (the current query also scans the joined category name `c.name`).
- Behavioral change: `FULLTEXT` uses tokenized word matching, not substring matching. Users currently find `"comp"` matching `"computer"`. Switching to word-boundary FULLTEXT would change that semantic without a UI/UX change to communicate it.
- Current catalog size does not yet justify the write-amplification cost of maintaining a FULLTEXT index on a mutated text column.

**Trigger for revisiting:** catalog exceeds 50 000 products and keyword search p95 response time consistently exceeds 500 ms on a production-sized MySQL instance.

### Application-level inverted index (Elasticsearch / MeiliSearch / Lucene)

A dedicated search engine would provide superior ranking, fuzzy matching, faceted filtering, and horizontal scalability.

**Rejected for now because:**
- Introduces an external infrastructure dependency not present in the current deployment model.
- Adds significant operational complexity (index synchronization, consistency on write, service discovery).
- The course-project scope and current traffic level do not justify this complexity.
- The problem is bounded: adding composite DB indexes provides a proportional improvement within the existing architecture at near-zero operational cost.

**Trigger for revisiting:** the project transitions to a multi-service deployment model, or search quality requirements (ranking, fuzzy matching, synonyms) cannot be satisfied within SQL semantics.

### Trigram index (pg_trgm / MySQL ngram FULLTEXT)

PostgreSQL's `pg_trgm` extension provides GiST/GIN indexes that support `LIKE '%kw%'` with index backing. MySQL 5.7+ supports an `ngram` parser for FULLTEXT that offers similar coverage for CJK and short tokens.

**Rejected for now because:**
- The project uses MySQL; `ngram` FULLTEXT has the same H2 incompatibility as standard FULLTEXT (see above).
- `ngram` FULLTEXT indexes are expensive to maintain for high-write tables and add significant storage overhead.
- Behavior differs from `LIKE '%kw%'` for short tokens and punctuation edge cases.

**Trigger for revisiting:** same as FULLTEXT — catalog > 50 000 products with measurable search latency regression.

### Query normalization (prefix-anchored LIKE)

Changing `LIKE '%kw%'` to `LIKE 'kw%'` (prefix-anchored) would allow the existing single-column index on `title` to be used. This would make keyword matching index-backed for title at the cost of no longer finding mid-word matches.

**Rejected for now because:**
- This is a behavioral change to search semantics. Users who search `"note"` expecting to find `"notebook"` would not be affected, but searching `"book"` expecting `"notebook"` results would silently return fewer or no results.
- Making this change without a UI/UX update to communicate the narrower semantics would be confusing.
- It solves only one column (`title`), not `subtitle`, `description`, or the joined `c.name`.

**Trigger for revisiting:** explicit product decision to shift to prefix-based search semantics (e.g., autocomplete-style search where users are expected to type from the beginning of a word).

## Trigger for Revisiting This Decision

Any of the following should prompt re-evaluation of the keyword search strategy:

1. Production MySQL catalog exceeds 50 000 products and keyword search p95 > 500 ms.
2. A dedicated search-engine infrastructure (Elasticsearch, MeiliSearch) is introduced for another feature and can be reused here.
3. H2 test compatibility is replaced or isolated (e.g., integration tests run against a containerized MySQL), removing the FULLTEXT/ngram syntax blocker.
4. A product decision changes keyword search semantics (e.g., word-boundary matching, fuzzy matching, relevance ranking).
