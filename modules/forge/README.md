# VoxelDash — Forge module

Forge (MinecraftForge) port of VoxelDash. Mirrors the Fabric module pipe-for-pipe,
but is written against **official Mojang mappings** (`ServerPlayer`,
`server.getPlayerList()`, `Level`, `Component`, …) and the Forge event bus.

## Building

```bash
./gradlew build
```

The VoxelDash shared API is pulled from your local Maven repository, exactly like
the Fabric module. Build/install it first from `modules/api`:

```bash
cd ../api && mvn install
```

The web UI is built automatically (`pnpm install && pnpm run build` against
`../../ui`) and folded into the jar by the `copyWebUI` task. The final relocated
fat jar lands in `build/libs/voxeldash-forge-<version>.jar`.

## Targeting a different Minecraft version

The source only touches APIs that are stable across the whole Forge 1.21.x line,
so retargeting is normally just two values in `gradle.properties`:

```properties
minecraft_version=1.21.1
forge_version=52.1.0
mapping_version=1.21.1
```

Pick a matching Minecraft/Forge pair from <https://files.minecraftforge.net/>. The
loader-facing version ranges in `META-INF/mods.toml` are intentionally wide
(`[1.21,1.22)`) so a single jar loads across the entire 1.21.x family.

> Forge 1.21 runs on official names at runtime, so no SRG re-obfuscation is
> needed. If you ever retarget an older Forge (≤ 1.20.1) that still ships SRG
> names, re-enable the `reobf { shadowJar {} }` block noted in `build.gradle`.

## What's implemented

- **All router pipes**: `ServerInfo`, `QuickAction`, `OnlinePlayer`, `Operator`,
  `Ban`, `Whitelist`, `World`, `Resource` (mods + datapacks).
- **Account command**: `/voxeldash password <password>` — creates (or updates) the
  operator's VoxelDash account, same as the other modules.
- **Console streaming**, **schedule actions**, and the full **dashboard widget**
  set (memory / CPU / TPS / uptime / world & system info).
