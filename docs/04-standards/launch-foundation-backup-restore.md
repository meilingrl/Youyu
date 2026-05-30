# Launch Foundation MySQL Backup and Restore Rehearsal

## Purpose

This runbook covers the Launch Foundation full-backup and guarded restore rehearsal for a self-hosted MySQL instance. It does not replace a managed database backup policy, incremental backup design, encryption, alerting, or off-site storage.

The scripts are Linux Shell utilities:

- `scripts/backup-mysql.sh`
- `scripts/restore-mysql.sh`

## Prerequisites

- Bash, `mysqldump`, `mysql`, and `gzip`
- A MySQL account with permission to dump the source database
- For restore rehearsals, permission to create a temporary database
- Credentials supplied through `MYSQL_PWD` or `MYSQL_ROOT_PASSWORD`

Supported environment variables:

| Variable | Default | Purpose |
| --- | --- | --- |
| `MYSQL_HOST` | `localhost` | MySQL host |
| `MYSQL_PORT` | `3306` | MySQL port |
| `MYSQL_USER` | `root` | MySQL user |
| `MYSQL_PWD` | unset | Preferred password input for MySQL CLI tools |
| `MYSQL_ROOT_PASSWORD` | unset | Fallback password input, useful with Compose env files |
| `DB_NAME` | `youyu` | Source database for backup |
| `BACKUP_DIR` | `./backups/mysql` | Backup output directory |
| `RETENTION_DAYS` | `7` | Delete matching backups older than this many days |

## Create a Backup

Run from the repository root:

```bash
MYSQL_PWD='replace-me' \
BACKUP_DIR='./backups/mysql' \
bash scripts/backup-mysql.sh
```

The script writes a UTC timestamped gzip file such as:

```text
./backups/mysql/youyu_20260530T120000Z.sql.gz
```

The dump is first written to a temporary file and renamed only after `mysqldump` and `gzip` succeed. Afterward, matching backup files older than `RETENTION_DAYS` are deleted. The default retention is 7 days.

For a daily local rehearsal schedule:

```cron
0 2 * * * cd /opt/youyu && MYSQL_PWD='replace-me' BACKUP_DIR='/var/backups/youyu/mysql' bash scripts/backup-mysql.sh >> /var/log/youyu-mysql-backup.log 2>&1
```

Do not commit credentials or place production credentials directly in a checked-in cron file. Use host secret management for real deployments.

## Restore into a Temporary Database

The restore script intentionally refuses the live-style database name `youyu`. It only accepts a new database whose name matches `youyu_restore_[A-Za-z0-9_]+`.

```bash
MYSQL_PWD='replace-me' \
bash scripts/restore-mysql.sh \
  --backup-file './backups/mysql/youyu_20260530T120000Z.sql.gz' \
  --target-db 'youyu_restore_20260530'
```

The target database must not already exist. After validation, remove the rehearsal database explicitly:

```bash
MYSQL_PWD='replace-me' \
mysql --host=localhost --port=3306 --user=root \
  --execute='DROP DATABASE `youyu_restore_20260530`;'
```

## Verification Commands

Syntax-check both scripts:

```bash
bash -n scripts/backup-mysql.sh
bash -n scripts/restore-mysql.sh
```

Verify the guard without connecting to MySQL:

```bash
bash scripts/restore-mysql.sh \
  --backup-file './backups/mysql/example.sql.gz' \
  --target-db 'youyu'
```

Expected result: the script exits non-zero and reports that it refuses to restore over the protected database.

## Rehearsal Evidence Template

Copy this section into the integration record when running a real rehearsal:

```markdown
### MySQL backup and restore rehearsal

- Date and operator:
- Environment:
- Source database:
- Backup command:
- Backup file:
- Backup file size:
- Backup completed successfully: yes / no
- Restore command:
- Temporary target database:
- Restore completed successfully: yes / no
- Validation queries and results:
- Protected `youyu` restore guard verified: yes / no
- Temporary database removed: yes / no
- Findings and follow-up tasks:
```
