#!/usr/bin/env bash
# Roda os perfis em sequência e junta os resultados em out/results.md.
set -uo pipefail
LOAD=$(cd "$(dirname "$0")" && pwd)
: >"$LOAD/out/results.md"
for profile in "${@:-render-free render-starter render-starter-1g render-standard vps-2cpu-4g}"; do
  for p in $profile; do
    echo "=== $p" >&2
    "$LOAD/run.sh" "$p" >/dev/null 2>"$LOAD/out/run-$p.log"
    cat "$LOAD/out/result-$p.md" >>"$LOAD/out/results.md" 2>/dev/null || echo "## $p: falhou (ver run-$p.log)" >>"$LOAD/out/results.md"
    echo >>"$LOAD/out/results.md"
  done
done
