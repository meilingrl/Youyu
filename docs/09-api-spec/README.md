# API Specification Directory

This directory stores the formal API contract documents for `CampusMarket`.

## Current Modules

- `auth.md`: authentication and current-user context
- `product.md`: product browse, detail, publish, and seller operations
- `order.md`: buyer order flow and admin order operations
- `report.md`: user-side report submission and admin-side report processing
- `search.md`: hot ranking, suggestions, governance rules, and search logs
- `user.md`: authenticated user profile, preference, verification, address, and insight endpoints
- `admin.md`: admin-side governance, review, and operational endpoints

## Source-Of-Truth Layers

The repository currently maintains three API-related layers with different responsibilities:

1. Runtime truth
   - `backend/src/main/java/com/campusmarket/backend/controller/**`
   - `backend/src/main/java/com/campusmarket/backend/controller/advice/GlobalExceptionHandler.java`
   - `backend/src/main/java/com/campusmarket/backend/common/api/ApiResponse.java`
   - `backend/src/main/java/com/campusmarket/backend/common/api/ResultCode.java`
2. Formal contract
   - `docs/09-api-spec/*.md`
   - Stable caller-facing endpoint contracts, field definitions, response envelope rules, and error semantics
3. Executable validation assets
   - `docs/06-http/*.http`
   - Manual integration, smoke validation, and request examples

Do not treat `.http` files as the formal contract layer. They are validation assets that should follow the code and the formal specs.

## What Belongs Here

- endpoint contract overviews
- request and response field definitions
- authentication and authorization rules
- response envelope rules
- `ResultCode` and HTTP status behavior that callers must understand
- business-rule notes that affect callers
- future OpenAPI / Swagger exports when the project introduces them

## Maintenance Rules

- When a core module gains new endpoints, add or expand the relevant formal spec before updating the matching `.http` file.
- When an existing endpoint contract changes, update controller behavior, the module spec, and the relevant `.http` file in the same iteration.
- Write these documents from the caller's perspective and keep examples close to current controller behavior.
- Record known drift explicitly instead of silently assuming the docs are correct.
- Do not turn these files into test scripts, design diaries, or implementation notes.

## Current Process

- The current repository process uses hand-written module specs based on the shared template in this directory.
- If the project later adopts OpenAPI / Swagger generation, generated artifacts should be treated as an additional export layer, not a replacement for module-level caller guidance.

## Relationship To Other Docs

- `../06-http/`: executable request samples and manual validation assets
- this directory: formal interface contracts
- `../08-tasks/`: task records for ongoing API standardization work
