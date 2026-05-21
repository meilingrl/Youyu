# Contributing

## Branching Strategy

```
main              -> always runnable; merges only
feature/<id>      -> one feature or task branch
fix/<short-desc>  -> targeted bug-fix branch
docs/<short-desc> -> documentation-only restructuring branch
```

Never commit directly to `main`.

## Commit Message Format

```text
<type>(<scope>): <imperative description>
```

Examples:

```text
feat(review): add product review summary endpoint
fix(search): exclude hidden keywords from hot ranking
docs(standards): reorganize docs tree and add AGENTS.md
```

## Workflow

1. Read the relevant task spec in `../08-tasks/`.
2. Implement within the task scope.
3. Update docs and API files in the same change when needed.
4. Run relevant checks.
5. Update `../CHANGELOG.md`.
6. Move the task to `archived/` when complete.

## Merge Checklist

- Backend tests pass when backend behavior changed.
- Frontend build passes when frontend changed.
- Relevant `../06-http/*.http` files are updated when endpoint behavior changed.
- No duplicate task docs are left active.
- `../CHANGELOG.md` contains a top entry for substantive work.
