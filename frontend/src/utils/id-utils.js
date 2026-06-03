const INVALID_ID_LITERALS = new Set(['nan', 'undefined', 'null', 'infinity', '-infinity'])

/**
 * Whether a route or API path segment is a positive integer entity id.
 * Rejects JavaScript artifacts such as NaN/undefined/null string forms.
 */
export function isValidEntityId(value) {
  if (value == null) {
    return false
  }

  const text = String(value).trim()
  if (!text || INVALID_ID_LITERALS.has(text.toLowerCase())) {
    return false
  }

  if (!/^\d+$/.test(text)) {
    return false
  }

  const numeric = Number(text)
  return Number.isSafeInteger(numeric) && numeric > 0
}

/**
 * Normalize a valid entity id to its canonical string form.
 */
export function normalizeEntityId(value) {
  return isValidEntityId(value) ? String(value).trim() : null
}

/**
 * Parse an optional positive integer id for query/body fields.
 * Returns undefined when the value is missing or invalid.
 */
export function parseOptionalEntityId(value) {
  return isValidEntityId(value) ? Number(String(value).trim()) : undefined
}

const ID_QUERY_PARAM_PATTERN =
  /(Id$|^id$|categoryId|orderId|shopId|productId|conversationId|ticketId|reportId|verificationId|couponId|activityId|assetId|cartItemId|addressId|refundId|ruleId|caseId|userId)/i

/**
 * Drop invalid numeric query params before they reach the backend.
 */
export function sanitizeRequestParams(params) {
  if (!params || typeof params !== 'object') {
    return params
  }

  const sanitized = {}

  for (const [key, value] of Object.entries(params)) {
    if (value == null || value === '') {
      continue
    }

    if (typeof value === 'number' && !Number.isFinite(value)) {
      continue
    }

    const text = String(value).trim()
    if (!text || INVALID_ID_LITERALS.has(text.toLowerCase())) {
      continue
    }

    if (key === 'page' || key === 'pageSize') {
      const numeric = Number(text)
      if (!Number.isFinite(numeric) || numeric < 1) {
        continue
      }
      sanitized[key] = Math.floor(numeric)
      continue
    }

    if (ID_QUERY_PARAM_PATTERN.test(key) && !isValidEntityId(value)) {
      continue
    }

    sanitized[key] = value
  }

  return sanitized
}
