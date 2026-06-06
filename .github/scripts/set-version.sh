#!/usr/bin/env bash
set -euo pipefail

VERSION="${1:?usage: set-version.sh <version>}"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

python3 - "$VERSION" "$ROOT" <<'PY'
import re, sys, pathlib

version, root = sys.argv[1], pathlib.Path(sys.argv[2])
pattern = re.compile(r'(<artifactId>voxeldash-[a-z]+</artifactId>\s*<version>)[^<]+(</version>)')

for rel in ['modules/api/pom.xml',
            'modules/spigot/pom.xml',
            'modules/bungeecord/pom.xml',
            'modules/vanilla/pom.xml']:
    pom = root / rel
    text = pom.read_text()
    updated, n = pattern.subn(lambda m: m.group(1) + version + m.group(2), text)
    pom.write_text(updated)
    print(f"{rel}: rewrote {n} voxeldash version(s) -> {version}")

def msi_safe(v):
    m = re.match(r'^(\d+\.\d+\.\d+)(?:-(.+))?$', v)
    if not m or not m.group(2):
        return v
    nums = re.findall(r'\d+', m.group(2))
    return f"{m.group(1)}-{nums[-1]}" if nums else m.group(1)

json_versions = {
    'ui/package.json': version,
    'one/package.json': version,
    'desktop/package.json': version,
    'desktop/src-tauri/tauri.conf.json': msi_safe(version),
}
for rel, ver in json_versions.items():
    pkg = root / rel
    text = pkg.read_text()
    text, n = re.subn(r'("version"\s*:\s*")[^"]*(")',
                      lambda m, ver=ver: m.group(1) + ver + m.group(2), text, count=1)
    pkg.write_text(text)
    print(f"{rel}: set version -> {ver} ({n})")
PY

CARGO="$ROOT/desktop/src-tauri/Cargo.toml"
sed -i -E "0,/^version = \".*\"/s//version = \"$VERSION\"/" "$CARGO"
echo "desktop/src-tauri/Cargo.toml: set version -> $VERSION"

for MODULE in fabric/mc1.8 fabric/mc1.12 fabric/mc1.14 fabric/mc1.16 fabric/mc1.20 fabric/mc1.21 forge/mc1.8 forge/mc1.12 forge/mc1.16 forge/mc1.20 forge/mc1.21 forge/mc26; do
  GP="$ROOT/modules/$MODULE/gradle.properties"
  sed -i -E "s/^mod_version=.*/mod_version=$VERSION/" "$GP"
  sed -i -E "s/^voxeldash_version=.*/voxeldash_version=$VERSION/" "$GP"
  echo "modules/$MODULE/gradle.properties: mod_version & voxeldash_version -> $VERSION"
done
