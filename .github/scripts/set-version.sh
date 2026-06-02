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

pkg = root / 'ui/package.json'
text = pkg.read_text()
text, n = re.subn(r'("version"\s*:\s*")[^"]*(")',
                  lambda m: m.group(1) + version + m.group(2), text, count=1)
pkg.write_text(text)
print(f"ui/package.json: set version -> {version} ({n})")
PY

GP="$ROOT/modules/fabric/gradle.properties"
sed -i -E "s/^mod_version=.*/mod_version=$VERSION/" "$GP"
sed -i -E "s/^voxeldash_version=.*/voxeldash_version=$VERSION/" "$GP"
echo "modules/fabric/gradle.properties: mod_version & voxeldash_version -> $VERSION"
