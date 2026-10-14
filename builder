#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

usage() {
  cat <<'EOF'
Uso: ./builder <comando>

Comandos:
  build    Compila o plugin (./gradlew buildPlugin)
  clean    Limpa artefatos de build
  help     Mostra esta ajuda
EOF
}

cmd="${1:-}"
case "$cmd" in
  build)
    ./gradlew buildPlugin --no-daemon
    zip_path="$(ls -t "$ROOT/build/distributions/"*.zip 2>/dev/null | head -1 || true)"
    if [[ -n "${zip_path:-}" && -f "$zip_path" ]]; then
      echo ""
      echo "Plugin gerado: $zip_path"
    fi
    ;;
  clean)
    ./gradlew clean --no-daemon
    ;;
  help | -h | --help)
    usage
    ;;
  "")
    echo "Erro: informe um comando." >&2
    echo "" >&2
    usage >&2
    exit 1
    ;;
  *)
    echo "Erro: comando desconhecido: $cmd" >&2
    echo "" >&2
    usage >&2
    exit 1
    ;;
esac
