#!/usr/bin/env bash
set -euo pipefail

REPO="gnmyt/VoxelDash"
CHANNEL="${CHANNEL:-stable}"
VERSION="${VERSION:-}"
INSTALL_DIR="${INSTALL_DIR:-}"
BIN_DIR="${BIN_DIR:-}"
PORT="${PORT:-7867}"
WITH_SERVICE=1

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
    awk 'NR==1{next} /^#/{sub(/^# ?/,""); print; next} {exit}' "$0"
    exit 0
}

while [ $# -gt 0 ]; do
    case "$1" in
        --beta)         CHANNEL="beta" ;;
        --stable)       CHANNEL="stable" ;;
        --version)      VERSION="${2:-}"; shift ;;
        --version=*)    VERSION="${1#*=}" ;;
        --dir)          INSTALL_DIR="${2:-}"; shift ;;
        --dir=*)        INSTALL_DIR="${1#*=}" ;;
        --bin)          BIN_DIR="${2:-}"; shift ;;
        --bin=*)        BIN_DIR="${1#*=}" ;;
        --no-service)   WITH_SERVICE=0 ;;
        --service)      WITH_SERVICE=1 ;;
        --help|-h)      usage ;;
        *)              die "Unknown option: $1 (try --help)" ;;
    esac
    shift
done

for tool in curl tar; do
    command -v "$tool" >/dev/null 2>&1 || die "'$tool' is required but not installed."
done

OS="$(uname -s)"
ARCH="$(uname -m)"

[ "$OS" = "Linux" ] || die "This installer only supports Linux. On Windows, download the .zip from https://github.com/$REPO/releases."

case "$ARCH" in
    x86_64|amd64) TARGET="linux-x64" ;;
    *)            die "Unsupported architecture '$ARCH'. Only x86_64 is published right now." ;;
esac

if [ "$(id -u)" -eq 0 ]; then
    : "${INSTALL_DIR:=/opt/voxeldash-one}"
    : "${BIN_DIR:=/usr/local/bin}"
else
    : "${INSTALL_DIR:=$HOME/.local/share/voxeldash-one}"
    : "${BIN_DIR:=$HOME/.local/bin}"
    if [ "$WITH_SERVICE" -eq 1 ]; then
        warn "Skipping the systemd service: that needs root. Re-run with sudo for a background service that starts on boot."
        WITH_SERVICE=0
        NO_SERVICE_REASON="root"
    fi
fi
NO_SERVICE_REASON="${NO_SERVICE_REASON:-}"

api_get() {
    curl -fsSL -H "Accept: application/vnd.github+json" \
        ${GITHUB_TOKEN:+-H "Authorization: Bearer $GITHUB_TOKEN"} "$1"
}

resolve_tag() {
    if [ -n "$VERSION" ]; then
        case "$VERSION" in v*) echo "$VERSION" ;; *) echo "v$VERSION" ;; esac
        return
    fi

    if [ "$CHANNEL" = "beta" ]; then
        api_get "https://api.github.com/repos/$REPO/releases?per_page=1" \
            | grep -m1 '"tag_name":' | sed -E 's/.*"tag_name":[[:space:]]*"([^"]+)".*/\1/'
    else
        api_get "https://api.github.com/repos/$REPO/releases/latest" \
            | grep -m1 '"tag_name":' | sed -E 's/.*"tag_name":[[:space:]]*"([^"]+)".*/\1/'
    fi
}

info "Resolving ${BOLD}$CHANNEL${RESET} release for $REPO..."
TAG="$(resolve_tag)"
[ -n "$TAG" ] || die "Could not resolve a release tag. Check the channel/version and your connection."

PLAIN="${TAG#v}"
ASSET="voxeldash-one-${PLAIN}-${TARGET}.tar.gz"
URL="https://github.com/$REPO/releases/download/$TAG/$ASSET"

info "Selected ${BOLD}$TAG${RESET} ($TARGET)"

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

info "Downloading $ASSET..."
curl -fSL --progress-bar "$URL" -o "$TMP/$ASSET" \
    || die "Download failed. Does $TAG ship a $TARGET build? See $URL"

info "Unpacking..."
tar -xzf "$TMP/$ASSET" -C "$TMP"

SRC="$(find "$TMP" -maxdepth 1 -type d -name 'voxeldash-one-*' | head -n1)"
[ -n "$SRC" ] && [ -f "$SRC/voxeldash-one" ] || die "Archive layout was not what we expected."

info "Installing into $INSTALL_DIR"
mkdir -p "$INSTALL_DIR"
cp "$SRC/voxeldash-one" "$INSTALL_DIR/voxeldash-one"
chmod +x "$INSTALL_DIR/voxeldash-one"
rm -rf "$INSTALL_DIR/ui"
cp -r "$SRC/ui" "$INSTALL_DIR/ui"

mkdir -p "$BIN_DIR"
ln -sf "$INSTALL_DIR/voxeldash-one" "$BIN_DIR/voxeldash-one"

if [ "$WITH_SERVICE" -eq 1 ] && ! command -v systemctl >/dev/null 2>&1; then
    warn "systemd was not found, skipping the service. You will have to start VoxelDash One yourself."
    WITH_SERVICE=0
    NO_SERVICE_REASON="no-systemd"
fi

if [ "$WITH_SERVICE" -eq 1 ]; then
    info "Setting up the voxeldash-one systemd service"
    cat > /etc/systemd/system/voxeldash-one.service <<EOF
[Unit]
Description=VoxelDash One
After=network.target

[Service]
Type=simple
WorkingDirectory=$INSTALL_DIR
ExecStart=$INSTALL_DIR/voxeldash-one
Restart=always
Environment=PORT=$PORT

[Install]
WantedBy=multi-user.target
EOF
    systemctl daemon-reload
    systemctl enable voxeldash-one.service >/dev/null 2>&1 || true
    systemctl restart voxeldash-one.service
fi

echo
info "${GREEN}VoxelDash One $TAG is installed.${RESET}"
echo

if ! command -v voxeldash-one >/dev/null 2>&1; then
    warn "$BIN_DIR is not on your PATH. Add it with:"
    printf '    %sexport PATH="%s:$PATH"%s\n\n' "$DIM" "$BIN_DIR" "$RESET"
fi

if [ "$WITH_SERVICE" -eq 1 ]; then
    cat <<EOF
VoxelDash One is ${BOLD}running${RESET} as a service and will start on boot.

    Open    ${BOLD}http://localhost:$PORT${RESET}  and create your admin account
    Logs    ${DIM}journalctl -u voxeldash-one -f${RESET}
    Stop    ${DIM}systemctl stop voxeldash-one${RESET}
EOF
elif [ "$NO_SERVICE_REASON" = "root" ]; then
    cat <<EOF
Start it now with:

    ${BOLD}voxeldash-one${RESET}

or re-run this installer with ${BOLD}sudo${RESET} to have it run as a background
service that starts on boot. Then open ${BOLD}http://localhost:$PORT${RESET}.
EOF
else
    cat <<EOF
Start it with:

    ${BOLD}voxeldash-one${RESET}

Then open ${BOLD}http://localhost:$PORT${RESET} and create your admin account.
EOF
fi
