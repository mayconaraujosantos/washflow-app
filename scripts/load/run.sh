#!/usr/bin/env bash
# Teste de carga do Washflow com os limites de CPU/memória de um plano de hospedagem.
#
#   scripts/load/run.sh seed                 # banco novo + massa (API sem limite)
#   scripts/load/run.sh <perfil> [HOLD=5m]   # roda o dia de pico com os limites do perfil
#
# Perfis (API cpus/memória · Postgres cpus/memória), conforme os planos publicados:
#   render-free        0.1/512m · 0.1/256m   (web Free + Postgres Free)
#   render-starter     0.5/512m · 0.1/256m   (web Starter + Postgres Basic-256mb)
#   render-starter-1g  0.5/512m · 0.5/1g     (web Starter + Postgres Basic-1gb)
#   render-standard    1/2g     · 0.5/1g     (web Standard + Postgres Basic-1gb)
#   vps-2cpu-4g        2/2g     · 2/2g       (VPS de 2 vCPU/4 GB dividida entre os dois)
# Ou defina API_CPUS, API_MEM, DB_CPUS e DB_MEM com o perfil "custom".
set -euo pipefail

ROOT=$(cd "$(dirname "$0")/../.." && pwd)
LOAD="$ROOT/scripts/load"
OUT="$LOAD/out"
NETWORK=washflow-load
DB=washflow-load-db
API=washflow-load-api
VOLUME=washflow-load-pgdata
PORT=${PORT:-7100}
PG_IMAGE=docker.io/library/postgres:16-alpine
JRE_IMAGE=docker.io/library/eclipse-temurin:21-jre-alpine
K6_IMAGE=docker.io/grafana/k6:0.57.0
K6=washflow-load-k6
SECRET=load-test-secret-0123456789abcdef0123
mkdir -p "$OUT"

profile=${1:?uso: run.sh seed | <perfil>}
case "$profile" in
  seed | unlimited) API_CPUS=0 API_MEM=0 DB_CPUS=0 DB_MEM=0 ;;
  render-free) API_CPUS=0.1 API_MEM=512m DB_CPUS=0.1 DB_MEM=256m ;;
  render-starter) API_CPUS=0.5 API_MEM=512m DB_CPUS=0.1 DB_MEM=256m ;;
  render-starter-1g) API_CPUS=0.5 API_MEM=512m DB_CPUS=0.5 DB_MEM=1g ;;
  render-standard) API_CPUS=1 API_MEM=2g DB_CPUS=0.5 DB_MEM=1g ;;
  vps-2cpu-4g) API_CPUS=2 API_MEM=2g DB_CPUS=2 DB_MEM=2g ;;
  custom) : "${API_CPUS:?}" "${API_MEM:?}" "${DB_CPUS:?}" "${DB_MEM:?}" ;;
  *) echo "perfil desconhecido: $profile" >&2; exit 2 ;;
esac

limits() { # cpus memory -> podman flags (0 = sem limite)
  [ "$1" = 0 ] || printf -- '--cpus=%s --memory=%s ' "$1" "$2"
}

start_db() {
  podman rm -f "$DB" >/dev/null 2>&1 || true
  # shellcheck disable=SC2046
  podman run -d --name "$DB" --network "$NETWORK" $(limits "$DB_CPUS" "$DB_MEM") \
    -e POSTGRES_PASSWORD=carga -e POSTGRES_DB=washflow -v "$VOLUME:/var/lib/postgresql/data" \
    "$PG_IMAGE" >/dev/null
  until podman exec "$DB" pg_isready -U postgres -d washflow >/dev/null 2>&1; do sleep 1; done
}

start_api() {
  podman rm -f "$API" >/dev/null 2>&1 || true
  local started=$SECONDS
  # shellcheck disable=SC2046
  podman run -d --name "$API" --network "$NETWORK" -p "$PORT:7000" $(limits "$API_CPUS" "$API_MEM") \
    -v "$ROOT/build/install/washflow-api:/app:ro" -e PORT=7000 -e APP_ENV=production \
    -e DB_HOST="$DB" -e DB_PORT=5432 -e DB_NAME=washflow -e DB_USER=postgres -e DB_PASSWORD=carga \
    -e AUTH_TOKEN_SECRET="$SECRET" -e SETUP_CODE=carga -e LOG_LEVEL=WARN \
    "$JRE_IMAGE" /app/bin/washflow-api >/dev/null
  until curl -sf "http://localhost:$PORT/api/health" >/dev/null; do
    if [ $((SECONDS - started)) -gt 600 ]; then echo "API não subiu em 10 min" >&2; exit 1; fi
    sleep 1
  done
  STARTUP=$((SECONDS - started))
  echo "API no ar em ${STARTUP}s"
}

podman network exists "$NETWORK" || podman network create "$NETWORK" >/dev/null
(cd "$ROOT" && ./gradlew installDist -q)

if [ "$profile" = seed ]; then
  podman rm -f "$API" "$DB" >/dev/null 2>&1 || true
  podman volume rm -f "$VOLUME" >/dev/null 2>&1 || true
  start_db
  start_api
  python3 -I "$LOAD/seed_load.py" --api "http://localhost:$PORT" --db-container "$DB" \
    --out "$OUT/tokens.json"
  exit 0
fi

[ -f "$OUT/tokens.json" ] || { echo "rode antes: scripts/load/run.sh seed" >&2; exit 1; }
podman rm -f "$K6" >/dev/null 2>&1 || true
start_db
# Each profile starts from the seeded state: drop the cars earlier runs pushed through the flow
# (plates KZ*), so a profile that failed mid-flow doesn't leave a longer queue for the next one.
podman exec -i "$DB" psql -U postgres -d washflow -q -v ON_ERROR_STOP=1 >/dev/null <<'SQL'
CREATE TEMP TABLE carga AS
  SELECT a.id FROM atendimentos a JOIN veiculos v ON v.id = a.veiculo_id WHERE v.placa LIKE 'KZ%';
DELETE FROM comissoes_lavador WHERE atendimento_id IN (SELECT id FROM carga);
DELETE FROM pagamentos WHERE atendimento_id IN (SELECT id FROM carga);
DELETE FROM atendimentos WHERE id IN (SELECT id FROM carga);
DELETE FROM veiculos WHERE placa LIKE 'KZ%';
VACUUM ANALYZE;
SQL
start_api

stats="$OUT/stats-$profile.jsonl"
: >"$stats"
( while true; do
    podman stats --no-stream --format json "$API" "$DB" >>"$stats" 2>/dev/null || true
    sleep 2
  done ) &
sampler=$!
trap 'kill $sampler 2>/dev/null || true' EXIT

rm -f "$OUT/summary-$profile.json"
podman run --rm --name "$K6" --network host --user 0 -v "$LOAD:/load:Z" \
  -e BASE_URL="http://localhost:$PORT" -e HOLD="${HOLD:-5m}" -e FLOWS_PER_MIN="${FLOWS_PER_MIN:-10}" \
  "$K6_IMAGE" run --quiet --address "" --no-usage-report \
  --summary-export "/load/out/summary-$profile.json" /load/washflow.js || true
# Never leave a k6 behind hammering the API while the next profile starts.
podman rm -f "$K6" >/dev/null 2>&1 || true

kill $sampler 2>/dev/null || true
python3 -I "$LOAD/report.py" "$profile" "$STARTUP" "$OUT/summary-$profile.json" "$stats" \
  "API ${API_CPUS} CPU/${API_MEM} · DB ${DB_CPUS} CPU/${DB_MEM}" | tee "$OUT/result-$profile.md"
