#!/usr/bin/env sh
# Restores a dump made by db-backup.sh into $PGDATABASE, replacing the tables
# it contains. Stop the API first so nothing writes during the restore.
#
#   db-restore.sh --yes /backups/washflow-20261110T060000Z.dump
set -eu

if [ "${1:-}" != "--yes" ] || [ -z "${2:-}" ]; then
  echo "usage: $0 --yes <dump-file>" >&2
  echo "Replaces the data in database '${PGDATABASE:-?}' on '${PGHOST:-?}'." >&2
  exit 2
fi
dump="$2"
[ -f "$dump" ] || { echo "no such dump: $dump" >&2; exit 2; }

pg_restore --clean --if-exists --no-owner --single-transaction \
  --dbname="${PGDATABASE:?PGDATABASE is required}" "$dump"
echo "restored $dump into $PGDATABASE"
