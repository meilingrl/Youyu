# API Spec: `<module>`

## Document Info

- Status: draft | active | verified
- Source of truth:
  - controller:
  - request DTO:
  - shared response / error handling:
  - request sample:
  - related task:
- Last updated:

## Scope

Describe which endpoints are covered by this document and which related endpoints are intentionally excluded.

## Authentication And Roles

- Public / login required / admin only
- Token format or session rule
- If different endpoints in the module have different role requirements, list them explicitly

## Response Envelope

All current backend endpoints should describe whether they use the unified response envelope:

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "Request succeeded",
  "data": {},
  "traceId": "..."
}
```

## Error Semantics

### HTTP Status And `ResultCode`

| HTTP status | `ResultCode` | Meaning |
|---|---|---|
| `400` | `BAD_REQUEST` | Invalid request parameters or validation failure |
| `401` | `UNAUTHORIZED` | Authentication required or invalid token |
| `403` | `FORBIDDEN` | Authenticated but not allowed |
| `404` | `NOT_FOUND` | Resource does not exist |
| `500` | `INTERNAL_SERVER_ERROR` | Unhandled or server-side failure |

### `BUSINESS_ERROR`

- Current backend behavior returns `200 OK` with `success=false`, `code="BUSINESS_ERROR"`, and a business-facing message.
- Callers must not assume every failure is represented by a non-2xx HTTP status.

## Endpoints

### `<METHOD> <path>`

#### Purpose

#### Request

##### Query

| Field | Required | Type | Notes |
|---|---|---|---|
|  |  |  |  |

##### Path

| Field | Required | Type | Notes |
|---|---|---|---|
|  |  |  |  |

##### Body

| Field | Required | Type | Notes |
|---|---|---|---|
|  |  |  |  |

#### Response

- `data` shape:

| Field | Type | Notes |
|---|---|---|
|  |  |  |

#### Error Cases

- `400`:
- `401`:
- `403`:
- `404`:
- `500`:
- `BUSINESS_ERROR`:

## Shared Types / Enumerations

List shared field meanings, enums, and status transitions that appear across endpoints in this module.

## HTTP Asset Mapping

- Primary validation file:
- Additional related files:

## Known Drift Or Follow-Up Notes

- Record known mismatch between code, spec, and `.http` assets here until it is resolved.
- If there is currently no known drift, state that explicitly.
