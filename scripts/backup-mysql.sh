#!/usr/bin/env bash
set -euo pipefail

MYSQL_HOST="${MYSQL_HOST:-localhost}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_USER="${MYSQL_USER:-root}"
DB_NAME="${DB_NAME:-youyu}"
BACKUP_DIR="${BACKUP_DIR:-./backups/mysql}"
RETENTION_DAYS="${RETENTION_DAYS:-7}"
TIMESTAMP="$(date -u +%Y%m%dT%H%M%SZ)"
BACKUP_FILE="${BACKUP_DIR}/${DB_NAME}_${TIMESTAMP}.sql.gz"
TEMP_FILE="${BACKUP_FILE}.tmp"

if ! command -v mysqldump >/dev/null 2>&1; then
  echo "Error: mysqldump is required." >&2
  exit 1
fi

if ! command -v gzip >/dev/null 2>&1; then
  echo "Error: gzip is required." >&2
  exit 1
fi

if [[ ! "${RETENTION_DAYS}" =~ ^[0-9]+$ ]]; then
  echo "Error: RETENTION_DAYS must be a non-negative integer." >&2
  exit 1
fi

if [[ -z "${MYSQL_PWD:-}" && -n "${MYSQL_ROOT_PASSWORD:-}" ]]; then
  export MYSQL_PWD="${MYSQL_ROOT_PASSWORD}"
fi

mkdir -p "${BACKUP_DIR}"
trap 'rm -f "${TEMP_FILE}"' EXIT

mysqldump \
  --host="${MYSQL_HOST}" \
  --port="${MYSQL_PORT}" \
  --user="${MYSQL_USER}" \
  --single-transaction \
  --routines \
  --triggers \
  --events \
  "${DB_NAME}" |
  gzip -c > "${TEMP_FILE}"

mv "${TEMP_FILE}" "${BACKUP_FILE}"
trap - EXIT

find "${BACKUP_DIR}" \
  -type f \
  -name "${DB_NAME}_*.sql.gz" \
  -mtime "+${RETENTION_DAYS}" \
  -delete

echo "Backup completed: ${BACKUP_FILE}"
