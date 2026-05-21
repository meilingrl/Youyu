# Testing

## Testing Layers

Use the lowest-cost layer that can prove the behavior:

1. Backend integration tests
2. Frontend store or unit tests
3. HTTP validation through `../06-http/*.http`
4. Manual smoke checks

## Backend

Run from `backend/`:

```bash
mvnw.cmd test
```

Use integration tests for:

- new backend endpoints
- permission rules
- state transitions
- bug reproductions

## Frontend

Run from `frontend/`:

```bash
npm test
npm run build
```

Use frontend automated tests for:

- Pinia store actions
- route guards
- UI state behavior with mocked API layers

## API Validation

The canonical executable HTTP request collections now live in `../06-http/`.

Typical validation set:

- `../06-http/auth.http`
- `../06-http/admin.http`
- `../06-http/product.http`
- `../06-http/order.http`
- additional files such as `report.http`, `review.http`, `recommend.http`, `search.http` when relevant

## Task Completion Expectation

When a task changes behavior, the implementer should update:

- code
- tests or validation checks
- relevant `.http` files
- task status
- `../CHANGELOG.md` when substantive
