#!/usr/bin/env bash
# Throwaway environment for the Playwright end-to-end tests (web/e2e).
#
#   scripts/e2e-env.sh up     build the image from the working tree and start
#                             a fresh PostgreSQL + app pod on http://localhost:7100,
#                             with a Mailpit inbox (SMTP sink) on http://localhost:7125
#                             and a fake Evolution API (WhatsApp) on http://localhost:7182
#   scripts/e2e-env.sh reset  empty database again, same image (no rebuild)
#   scripts/e2e-env.sh down   remove the pod (and every byte of its data)
#
# Everything runs in one Podman pod, so it never touches the development app
# on :7000, its database or the build/ directory. Each `up` starts from an
# empty database, so the tests can go through /configurar like a new install.
#
# E2E_HOST: where the tests reach the pod (default localhost). On Windows the
# Podman VM doesn't forward localhost, so web/e2e passes the VM's IP and
# E2E_BIND=0.0.0.0 to publish the ports on it.
set -euo pipefail

POD=washflow-e2e
IMAGE=washflow-api:e2e
PORT="${E2E_PORT:-7100}"
MAIL_PORT="${E2E_MAIL_PORT:-7125}"
WHATSAPP_PORT="${E2E_WHATSAPP_PORT:-7182}"
HOST="${E2E_HOST:-localhost}"
BIND="${E2E_BIND:-127.0.0.1}"
BUN_IMAGE=docker.io/oven/bun:1.3.14-alpine
EVOLUTION_KEY=e2e-evolution-api-key
MAILPIT_IMAGE=docker.io/axllent/mailpit:v1.27.3
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUNTIME="${CONTAINER_RUNTIME:-podman}"

down() {
  "$RUNTIME" pod rm -f "$POD" >/dev/null 2>&1 || true
}

up() {
  echo "==> building $IMAGE from the working tree"
  "$RUNTIME" build -q -t "$IMAGE" "$ROOT" >/dev/null
  start
}

start() {
  down
  echo "==> starting PostgreSQL and the app on http://$HOST:$PORT"
  "$RUNTIME" pod create --name "$POD" -p "$BIND:$PORT:7000" -p "$BIND:$MAIL_PORT:8025" \
    -p "$BIND:$WHATSAPP_PORT:8080" >/dev/null
  # Catches every e-mail the app sends (SMTP :1025); the tests read it over HTTP.
  "$RUNTIME" run -d --pod "$POD" --name "$POD-mail" "$MAILPIT_IMAGE" >/dev/null
  # Stands in for the Evolution API: the app links numbers, sends and gets
  # webhooks through it; the tests drive it on /test/* (web/e2e/support).
  "$RUNTIME" run -d --pod "$POD" --name "$POD-whatsapp" \
    -e EVOLUTION_API_KEY="$EVOLUTION_KEY" \
    "$BUN_IMAGE" bun -e "$(cat "$ROOT/web/e2e/support/fake-evolution.mjs")" >/dev/null
  "$RUNTIME" run -d --pod "$POD" --name "$POD-db" \
    -e POSTGRES_DB=washflow -e POSTGRES_USER=washflow -e POSTGRES_PASSWORD=washflow \
    docker.io/library/postgres:16-alpine >/dev/null
  for _ in $(seq 1 60); do
    "$RUNTIME" exec "$POD-db" pg_isready -q -U washflow -d washflow && break
    sleep 1
  done

  # Development mode: the setup wizard needs no SETUP_CODE and WhatsApp
  # codes go to the log. The signing secret is new on every run.
  "$RUNTIME" run -d --pod "$POD" --name "$POD-app" \
    -e APP_ENV=development \
    -e DB_HOST=127.0.0.1 -e DB_PORT=5432 -e DB_NAME=washflow \
    -e DB_USER=washflow -e DB_PASSWORD=washflow \
    -e AUTH_TOKEN_SECRET="$(head -c 48 /dev/urandom | base64 | tr -d '\n')" \
    -e OTP_CHANNEL=log \
    -e SMTP_HOST=127.0.0.1 -e SMTP_PORT=1025 -e SMTP_SECURITY=none \
    -e SMTP_FROM="Washflow <nao-responda@washflow.test>" \
    -e EVOLUTION_URL=http://127.0.0.1:8080 -e EVOLUTION_API_KEY="$EVOLUTION_KEY" \
    -e WHATSAPP_WEBHOOK_URL=http://127.0.0.1:7000/api/whatsapp/webhook \
    -e WHATSAPP_WEBHOOK_TOKEN="$(head -c 32 /dev/urandom | base64 | tr -d '\n')" \
    "$IMAGE" >/dev/null

  for _ in $(seq 1 120); do
    if curl -fsS "http://$HOST:$PORT/api/health" 2>/dev/null | grep -q '"up"'; then
      echo "==> ready"
      return 0
    fi
    sleep 1
  done
  echo "app did not become healthy; last log lines:" >&2
  "$RUNTIME" logs --tail 40 "$POD-app" >&2 || true
  return 1
}

case "${1:-}" in
  up) up ;;
  reset) start ;;
  down) down ;;
  *) echo "usage: $0 up|reset|down" >&2; exit 2 ;;
esac
