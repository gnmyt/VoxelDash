#!/usr/bin/env bash
set -euo pipefail

DESKTOP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ROOT="$(cd "$DESKTOP_DIR/.." && pwd)"

TRIPLE="${1:-$(rustc -vV | sed -n 's/^host: //p')}"
if [[ -z "$TRIPLE" ]]; then
  echo "Could not determine the Rust target triple; pass it as the first argument." >&2
  exit 1
fi

case "$TRIPLE" in
  *windows*) BUN_TARGET="bun-windows-x64"; EXT=".exe" ;;
  *darwin*aarch64* | aarch64*darwin*) BUN_TARGET="bun-darwin-arm64"; EXT="" ;;
  *darwin*) BUN_TARGET="bun-darwin-x64"; EXT="" ;;
  *aarch64*linux* | aarch64*) BUN_TARGET="bun-linux-arm64"; EXT="" ;;
  *linux*) BUN_TARGET="bun-linux-x64"; EXT="" ;;
  *) echo "Unsupported target triple: $TRIPLE" >&2; exit 1 ;;
esac

BIN_DIR="$DESKTOP_DIR/src-tauri/binaries"
WEBUI_DIR="$DESKTOP_DIR/src-tauri/webui"
mkdir -p "$BIN_DIR"

echo "==> Building web UI (vite)"
( cd "$ROOT/ui" && pnpm install --frozen-lockfile && bunx vite build )

echo "==> Staging web UI -> $WEBUI_DIR"
rm -rf "$WEBUI_DIR"
mkdir -p "$WEBUI_DIR"
cp -r "$ROOT/ui/dist/." "$WEBUI_DIR/"

echo "==> Compiling One backend ($BUN_TARGET)"
( cd "$ROOT/one" && bun install --frozen-lockfile )
OUT="$BIN_DIR/voxeldash-one-$TRIPLE$EXT"
( cd "$ROOT/one" && bun build src/index.js --compile --minify --target="$BUN_TARGET" --outfile "$OUT" )

echo "==> Done:"
echo "    backend : $OUT"
echo "    webui   : $WEBUI_DIR"
