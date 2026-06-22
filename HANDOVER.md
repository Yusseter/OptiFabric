# OptiFabric 1.21.11 Handover

## Goal

Make OptiFabric run reliably with OptiFine on Minecraft `1.21.11`.

The working target is:

- Minecraft: `1.21.11`
- Java: `21`
- Fabric Loader: `0.17.3`
- Fabric API runtime: `0.141.4+1.21.11`
- OptiFabric: `1.15.2`
- OptiFine: `OptiFine_1.21.11_HD_U_J9.jar`
- Mixin subsystem: `0.8.7`
- MixinExtras: `0.5.0`

## Current Workflow

The workflow is now autonomous.

The repo contains a copied local Minecraft runtime under:

- `.local/minecraft-runtime/.minecraft`

Use that runtime for the normal build/test loop. Do not rely on the user manually launching Minecraft for each iteration.

Expected loop:

1. Inspect the current source, copied runtime, and available logs/artifacts.
2. Rebuild OptiFabric.
3. Install the rebuilt jar into `.local/minecraft-runtime/.minecraft/mods`.
4. Launch Minecraft from the local runtime using Java 21.
5. Capture logs, crashes, thread dumps, and class dumps directly.
6. Diagnose the next failure from those local artifacts.
7. Repeat until the local runtime reaches a useful running state.

The user should only be asked for a Windows run when:

- local Linux/WSL testing reaches a stable point,
- a problem looks platform-specific,
- or a real Windows confirmation is needed before considering the fix done.

## Local Runtime Rules

- Treat `.local/minecraft-runtime/.minecraft` as the primary test runtime.
- Do not modify the user's real `.minecraft` folder.
- Do not delete or move unrelated build artifacts.
- Do not delete old build outputs unless the user explicitly asks.
- Keep `.local/` untracked.
- The copied runtime may contain cached `.optifine` output and generated logs that are useful for autonomous diagnosis.

## Build

Use Java 21 explicitly. The system Java may be older.

Known working build command:

```sh
GRADLE_USER_HOME="$PWD/.local/gradle-home" ./gradlew build -Pjava21Home=/tmp/codex-jdk21/jdk
```

Primary jar:

```text
build/libs/optifabric-1.15.2.jar
```

Copied runtime mod path:

```text
.local/minecraft-runtime/.minecraft/mods/optifabric-1.15.2.jar
```

## Local Launch

Use the copied runtime and Java 21. A known useful launch pattern is:

```sh
MCROOT="$PWD/.local/minecraft-runtime/.minecraft"
CP="$(cat /tmp/optifabric-cp.txt)"
ALSOFT_DRIVERS=null timeout 240s /tmp/codex-jdk21/jdk/bin/java \
  -Xmx2G -Xms512M \
  -DFabricMcEmu=' net.minecraft.client.main.Main ' \
  -Djava.library.path="$MCROOT/natives" \
  -cp "$CP" \
  net.fabricmc.loader.impl.launch.knot.KnotClient \
  --username CodexOffline \
  --version fabric-loader-0.17.3-1.21.11 \
  --gameDir "$MCROOT" \
  --assetsDir "$MCROOT/assets" \
  --assetIndex 29 \
  --uuid 00000000000000000000000000000000 \
  --accessToken 0 \
  --userType msa \
  --versionType release
```

`ALSOFT_DRIVERS=null` avoids a WSL/OpenAL stall and should be used for local smoke tests.

## Debug Artifacts

The old user-assisted `debug/` folder can still be useful, but it is no longer the main feedback loop.

Use, in priority order:

1. Fresh logs and dumps generated from local autonomous launches.
2. `.local/minecraft-runtime/.minecraft/logs/latest.log`
3. `.local/minecraft-runtime/.minecraft/crash-reports/`
4. `.local/minecraft-runtime/.minecraft/.optifine/`
5. Any local run captures under `.local/run-captures/`
6. `debug/` only when the user has provided a fresh Windows run or explicitly asks to analyze it.

If `debug/` conflicts with freshly generated local evidence, trust the local evidence unless the issue is clearly Windows-specific.

## What Has Been Fixed

- `ZipUtils.transform(...)` now creates parent directories before writing mapped jars.
- OptiFine remapping/setup can generate and use `.optifine` cache output.
- The project builds as `1.15.2`.
- `tinyremapper` runtime dependency/classpath issues were addressed enough for local launch to pass earlier failures.
- Runtime patchers/backfills have been added or updated for several OptiFine-shaped Minecraft classes, including:
  - `class_776` / block render manager
  - `class_761` / world renderer
  - `class_1092` / baked model manager
  - `class_775` / fluid renderer
  - `class_309`
  - `class_702`
  - `class_10430`
- Excluded 1.21 compat mixin packages are skipped at runtime instead of registering mixin configs whose classes are not present in the 1.21 build.

## Important Current Understanding

- Most observed failures have been Minecraft `1.21.11` compatibility issues, not original OptiFabric bugs.
- OptiFine rewrites Minecraft classes enough that Fabric mixin anchors often need 1.21.11-specific backfills or guards.
- The old per-class ping-pong should be avoided where possible by inspecting local class dumps, mixin configs, and bytecode before asking the user to run Windows.
- Linux/WSL rendering/audio behavior is not a final correctness signal. It is good for Java/classloading/mixin/patcher verification.
- Windows remains the final platform confirmation because the user plays there.

## Safety Notes

- Do not write to the user's real `.minecraft`.
- Do not remove build outputs just to clean up.
- Keep changes focused on OptiFabric 1.21.11 compatibility.
- Rebuild after source changes and verify locally before reporting back.
