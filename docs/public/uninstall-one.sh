#!/usr/bin/env bash
set -euo pipefail

REPO="gnmyt/VoxelDash"
INSTALL_DIR="${INSTALL_DIR:-}"
BIN_DIR="${BIN_DIR:-}"
DATA_DIR="${VOXELDASH_HOME:-}"
PURGE=0
SERVICE_UNIT="/etc/systemd/system/voxeldash-one.service"

if [ -t 1 ]; then
    BOLD="$(printf '\033[1m')"; DIM="$(printf '\033[2m')"; RESET="$(printf '\033[0m')"
    GREEN="$(printf '\033[32m')"; RED="$(printf '\033[31m')"; YELLOW="$(printf '\033[33m')"
else
    BOLD=""; DIM=""; RESET=""; GREEN=""; RED=""; YELLOW=""
fi

info()  { printf '%s==>%s %s\n' "$GREEN$BOLD" "$RESET" "$*"; }
warn()  { printf '%s!  %s%s\n' "$YELLOW" "$*" "$RESET" >&2; }
die()   { printf '%sError:%s %s\n' "$RED$BOLD" "$RESET" "$*" >&2; exit 1; }

usage() {
    cat <<EOF
VoxelDash One uninstaller

  curl -fsSL https://voxeldash.dev/uninstall-one.sh | sudo bash

Stops and removes the systemd service, the binary, the bundled web UI and the
launcher on your PATH. Your data (account, servers, database) is kept unless you
pass --purge.

Options:

  --purge          Also delete the data folder (servers, database, Java runtimes)
  --dir <path>     Install directory to remove (auto-detected from the service)
  --bin <path>     Directory holding the launcher symlink
  --help           Show this help and exit
EOF
    exit 0
}

while [ $# -gt 0 ]; do
    case "$1" in
        --purge)        PURGE=1 ;;
        --dir)          INSTALL_DIR="${2:-}"; shift ;;
        --dir=*)        INSTALL_DIR="${1#*=}" ;;
        --bin)          BIN_DIR="${2:-}"; shift ;;
        --bin=*)        BIN_DIR="${1#*=}" ;;
        --help|-h)      usage ;;
        *)              die "Unknown option: $1 (try --help)" ;;
    esac
    shift
done

if [ -z "$INSTALL_DIR" ] && [ -f "$SERVICE_UNIT" ]; then
    EXEC_LINE="$(grep -m1 '^ExecStart=' "$SERVICE_UNIT" 2>/dev/null | cut -d= -f2-)"
    [ -n "$EXEC_LINE" ] && INSTALL_DIR="$(dirname "$EXEC_LINE")"
fi

if [ "$(id -u)" -eq 0 ]; then
    : "${INSTALL_DIR:=/opt/voxeldash-one}"
    : "${BIN_DIR:=/usr/local/bin}"
else
    : "${INSTALL_DIR:=$HOME/.local/share/voxeldash-one}"
    : "${BIN_DIR:=$HOME/.local/bin}"
fi

: "${DATA_DIR:=$INSTALL_DIR/data}"

if [ -f "$SERVICE_UNIT" ]; then
    if [ "$(id -u)" -eq 0 ]; then
        info "Stopping and removing the systemd service"
        if command -v systemctl >/dev/null 2>&1; then
            systemctl stop voxeldash-one.service >/dev/null 2>&1 || true
            systemctl disable voxeldash-one.service >/dev/null 2>&1 || true
        fi
        rm -f "$SERVICE_UNIT" || warn "Could not remove $SERVICE_UNIT"
        command -v systemctl >/dev/null 2>&1 && systemctl daemon-reload >/dev/null 2>&1 || true
    else
        warn "A systemd service exists at $SERVICE_UNIT, but removing it needs root. Re-run with sudo to remove it."
    fi
fi

LINK="$BIN_DIR/voxeldash-one"
if [ -L "$LINK" ] || [ -f "$LINK" ]; then
    info "Removing launcher $LINK"
    rm -f "$LINK"
fi

if [ "$PURGE" -eq 1 ]; then
    info "Removing $INSTALL_DIR (including data)"
    rm -rf "$INSTALL_DIR"
    if [ -e "$DATA_DIR" ] && [ "$DATA_DIR" != "$INSTALL_DIR/data" ]; then
        info "Removing data folder $DATA_DIR"
        rm -rf "$DATA_DIR"
    fi
else
    info "Removing the binary and web UI from $INSTALL_DIR"
    rm -f "$INSTALL_DIR/voxeldash-one"
    rm -rf "$INSTALL_DIR/ui"
    rmdir "$INSTALL_DIR" >/dev/null 2>&1 || true
fi

echo
info "${GREEN}VoxelDash One has been uninstalled.${RESET}"

if [ "$PURGE" -ne 1 ] && [ -e "$DATA_DIR" ]; then
    echo
    cat <<EOF
Your data was kept at ${BOLD}$DATA_DIR${RESET}.
Remove it too with:

    ${DIM}rm -rf "$DATA_DIR"${RESET}

or re-run this uninstaller with ${BOLD}--purge${RESET}.
EOF
fi
