#!/usr/bin/env sh
# Dumps the washflow database (pg_dump custom format) into $BACKUP_DIR and
# deletes dumps older than $BACKUP_KEEP_DAYS. Run by the washflow-db-backup
# CronJob (backup.yaml next to this folder); restore with db-restore.sh.
#
# Connection: the standard libpq variables (PGHOST, PGPORT, PGUSER, PGPASSWORD,
# PGDATABASE).
# Off-site copy: set BACKUP_S3_URI (e.g. s3://bucket/washflow) and the AWS_*
# variables (AWS_ENDPOINT_URL for R2/MinIO); needs the aws CLI in the image.
set -eu

BACKUP_DIR="${BACKUP_DIR:-/backups}"
BACKUP_KEEP_DAYS="${BACKUP_KEEP_DAYS:-14}"
stamp="$(date -u +%Y%m%dT%H%M%SZ)"
target="$BACKUP_DIR/washflow-$stamp.dump"

mkdir -p "$BACKUP_DIR"
# Written under another name first: a dump cut short (disk full, pod killed)
# must never look like a good backup.
pg_dump --format=custom --no-owner --file="$target.partial"
pg_restore --list "$target.partial" >/dev/null
mv "$target.partial" "$target"
echo "backup written: $target ($(du -h "$target" | cut -f1))"

if [ -n "${BACKUP_S3_URI:-}" ]; then
  aws s3 cp --only-show-errors "$target" "${BACKUP_S3_URI%/}/$(basename "$target")"
  echo "backup copied to ${BACKUP_S3_URI%/}/"
fi

find "$BACKUP_DIR" -name 'washflow-*.dump' -type f -mtime +"$BACKUP_KEEP_DAYS" \
  -print -delete | sed 's/^/pruned: /'
