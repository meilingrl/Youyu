#!/usr/bin/env bash
set -euo pipefail

MYSQL_HOST="${MYSQL_HOST:-localhost}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_USER="${MYSQL_USER:-root}"
BACKUP_FILE=""
TARGET_DB=""

usage() {
  cat <<'EOF'
Usage: restore-mysql.sh --backup-file <path.sql.gz> --target-db <youyu_restore_name>

Restores a gzip-compressed MySQL backup into a new rehearsal database.
The target database must start with "youyu_restore_" and must not exist.
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --backup-file)
      BACKUP_FILE="${2:-}"
      shift 2
      ;;
    --target-db)
      TARGET_DB="${2:-}"
      shift 2
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    *)
      echo "Error: unknown argument: $1" >&2
      usage >&2
      exit 1
      ;;
  esac
done

if [[ -z "${BACKUP_FILE}" || -z "${TARGET_DB}" ]]; then
  echo "Error: --backup-file and --target-db are required." >&2
  usage >&2
  exit 1
fi

if [[ "${TARGET_DB,,}" == "youyu" ]]; then
  echo "Error: refusing to restore over the protected database: ${TARGET_DB}" >&2
  exit 1
fi

if [[ ! "${TARGET_DB}" =~ ^youyu_restore_[A-Za-z0-9_]+$ ]]; then
  echo "Error: target database must match youyu_restore_[A-Za-z0-9_]+." >&2
  exit 1
fi

if [[ ! -f "${BACKUP_FILE}" ]]; then
  echo "Error: backup file does not exist: ${BACKUP_FILE}" >&2
  exit 1
fi

for command_name in mysql gzip; do
  if ! command -v "${command_name}" >/dev/null 2>&1; then
    echo "Error: ${command_name} is required." >&2
    exit 1
  fi
done

if [[ -z "${MYSQL_PWD:-}" && -n "${MYSQL_ROOT_PASSWORD:-}" ]]; then
  export MYSQL_PWD="${MYSQL_ROOT_PASSWORD}"
fi

MYSQL_ARGS=(
  "--host=${MYSQL_HOST}"
  "--port=${MYSQL_PORT}"
  "--user=${MYSQL_USER}"
  "--batch"
  "--skip-column-names"
)

TARGET_EXISTS="$(
  mysql "${MYSQL_ARGS[@]}" \
    --execute="SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = '${TARGET_DB}';"
)"

if [[ -n "${TARGET_EXISTS}" ]]; then
  echo "Error: target database already exists: ${TARGET_DB}" >&2
  exit 1
fi

mysql "${MYSQL_ARGS[@]}" \
  --execute="CREATE DATABASE \`${TARGET_DB}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

if ! gzip -dc "${BACKUP_FILE}" | mysql "${MYSQL_ARGS[@]}" "${TARGET_DB}"; then
  echo "Error: restore failed. Drop the rehearsal database before retrying: ${TARGET_DB}" >&2
  exit 1
fi

echo "Restore completed: ${BACKUP_FILE} -> ${TARGET_DB}"
