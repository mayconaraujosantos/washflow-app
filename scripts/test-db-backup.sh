#!/usr/bin/env sh
# End-to-end check of infra/gitops/apps/washflow/db-backup/*.sh: a throwaway
# postgres:16-alpine container (the same image the backup CronJob uses) gets a
# row, is backed up, loses the table, is restored and must have the row back.
#
#   scripts/test-db-backup.sh            # podman, or docker when podman is absent
#   CONTAINER_RUNTIME=docker scripts/test-db-backup.sh
set -eu

RUNTIME="${CONTAINER_RUNTIME:-$(command -v podman || command -v docker)}"
SCRIPTS="$(cd "$(dirname "$0")/../infra/gitops/apps/washflow/db-backup" && pwd)"
NAME="washflow-backup-test-$$"

cleanup() { "$RUNTIME" rm -f "$NAME" >/dev/null 2>&1 || true; }
trap cleanup EXIT

fail() { echo "FAIL: $*" >&2; exit 1; }
in_db() { "$RUNTIME" exec -e PGPASSWORD=test "$NAME" "$@"; }
sql() { in_db psql -U washflow -d washflow -tAc "$1"; }

"$RUNTIME" run -d --name "$NAME" \
  -e POSTGRES_USER=washflow -e POSTGRES_PASSWORD=test -e POSTGRES_DB=washflow \
  -v "$SCRIPTS:/scripts:ro,Z" docker.io/library/postgres:16-alpine >/dev/null

i=0
until in_db pg_isready -U washflow -d washflow -h 127.0.0.1 >/dev/null 2>&1; do
  i=$((i + 1)); [ "$i" -lt 60 ] || fail "postgres did not start"; sleep 1
done
sleep 1

sql "CREATE TABLE cash_transaction (id int primary key, valor numeric(10,2));
     INSERT INTO cash_transaction VALUES (1, 49.90);"

# A dump older than the retention must be pruned; a recent one kept.
in_db sh -c 'mkdir -p /backups &&
  touch -d "@$(( $(date +%s) - 30*86400 ))" /backups/washflow-20200101T000000Z.dump &&
  touch /backups/washflow-recent-manual.dump'

backup_env="-e PGHOST=127.0.0.1 -e PGUSER=washflow -e PGPASSWORD=test -e PGDATABASE=washflow
  -e BACKUP_DIR=/backups -e BACKUP_KEEP_DAYS=14"
# shellcheck disable=SC2086
"$RUNTIME" exec $backup_env "$NAME" sh /scripts/db-backup.sh || fail "backup exited non-zero"

dumps="$(in_db sh -c 'ls /backups')"
echo "$dumps" | grep -q '^washflow-[0-9]\{8\}T[0-9]\{6\}Z\.dump$' || fail "no timestamped dump: $dumps"
echo "$dumps" | grep -q 'washflow-20200101T000000Z.dump' && fail "old dump not pruned"
echo "$dumps" | grep -q 'washflow-recent-manual.dump' || fail "recent dump was pruned"
echo "$dumps" | grep -q 'partial' && fail "partial file left behind"

latest="$(in_db sh -c 'ls /backups/washflow-2*.dump | sort | tail -1')"

sql "DROP TABLE cash_transaction;"

# shellcheck disable=SC2086
if "$RUNTIME" exec $backup_env "$NAME" sh /scripts/db-restore.sh "$latest" >/dev/null 2>&1; then
  fail "restore ran without --yes"
fi
# shellcheck disable=SC2086
"$RUNTIME" exec $backup_env "$NAME" sh /scripts/db-restore.sh --yes "$latest" \
  || fail "restore exited non-zero"

[ "$(sql 'SELECT valor FROM cash_transaction WHERE id = 1')" = "49.90" ] \
  || fail "row not restored"

echo "OK: backup, retention and restore"
