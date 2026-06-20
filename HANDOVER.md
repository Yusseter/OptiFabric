# OptiFabric Debug Handover

## Goal

Get OptiFabric working on Minecraft 1.21.11 with OptiFine without crashing during startup, specifically past the `Reflector` initialization path that currently fails on `class_776`.

## Runtime matrix

- Minecraft: `1.21.11`
- Java: `21`
- Fabric Loader: `0.17.3`
- Fabric API at runtime: `0.141.4+1.21.11`
- OptiFabric: `1.15.1`
- OptiFine: `OptiFine_1.21.11_HD_U_J9.jar`
- Mixin subsystem: `0.8.7`
- MixinExtras: `0.5.0`
- Loom: `0.2.7-SNAPSHOT`

## Declared build versions

- `minecraft_version=1.21.11`
- `yarn_mappings=1.21.11+build.3`
- `loader_version=0.17.3`
- `fabric_version=0.141.3+1.21.11`
- `tiny_remapper_version=0.8.11`
- `fabric_asm_version=v2.3`
- `mod_version=1.15.1`
- `maven_group=me.modmuss50`
- `archives_base_name=optifabric`

## What we are doing

We are iterating on OptiFabric startup compatibility:

1. Rebuild the mod jar.
2. Replace the jar in the user’s mods folder.
3. Run Minecraft once.
4. Replace the repo `debug/` folder with the fresh output.
5. Re-read only the fresh `debug/` contents.
6. Apply the next targeted fix based on that run.

The working rule is that `debug/` is ephemeral and must not be treated as cached state.

## Current state

The latest fresh `debug/` run still crashes during OptiFine startup with:

- `java.lang.NoClassDefFoundError: Could not initialize class net.optifine.reflect.Reflector`
- root cause remains `java.lang.RuntimeException: Mixin transformation of net.minecraft.class_776 failed`

The current evidence says the failure is still centered on `class_776` during OptiFine's initialization path.

## What is fixed

1. The missing output directory bug is fixed.
   - `ZipUtils.transform(...)` now creates the parent directory before writing `Optifine-mapped.jar`.
   - This removed the earlier `FileNotFoundException` for `.optifine/.../Optifine-mapped.jar`.

2. The runtime patchers for OptiFine-shaped classes are in place.
   - `class_761` backfill exists.
   - `class_1092` backfill exists.
   - `class_776` backfill exists.

3. The class dumps confirm those fixers are taking effect in the transformed runtime jar.
   - `debug/optifabric-debug/net_minecraft_class_776.class`
   - `debug/optifabric-debug/net_minecraft_class_761.class`
   - `debug/optifabric-debug/net_minecraft_class_1092.class`
   - `debug/optifabric-debug/net_minecraft_class_309.class`

4. The build now succeeds under Java 21.

5. The build file still pins Fabric API module versions separately from the runtime log.
   - `gradle.properties` declares `fabric_version=0.141.3+1.21.11`
   - the fresh runtime log shows `fabric-api 0.141.4+1.21.11`
   - this is a real version mismatch to keep in mind when comparing build config to runtime state

## What we know from the fresh debug logs

1. `class_776` after patching contains:
   - `method_3351`
   - `method_23071`
   - `method_3355`
   - `method_3352`
   - `method_3350`
   - `method_3349`
   - `method_3353`
   - `renderBreakingTexture`
   - `renderSingleBlock`
   - `method_14491`

2. The fresh `debug/latest.log` still ends at the same `class_776` transformation failure.

3. The fresh `debug/optifabric-startup.log` does not show the renderer plugin debug strings that were added earlier.

## What is definitely not the issue anymore

1. The missing `.optifine/.../Optifine-mapped.jar` parent directory.
   - That was the `FileNotFoundException`.
   - It is fixed.

2. Missing basic methods on the transformed `class_776`.
   - The dumped class already contains the relevant methods.

3. `class_761` or `class_1092` being incomplete.
   - Their dumps show the fixers completed successfully.

4. The current failure being caused by `full_slabs` as an active mod path.
   - The fresh loaded-mod log did not show it.

## What is still suspect

1. Some mixin path is still trying to transform `class_776`.
   - The repo has direct `class_776` compat paths in:
     - `compat/fabricrendererapi`
     - `compat/full_slabs`
     - `compat/indigo`
   - The current runtime crash is still centered on that target class.

2. A mixin plugin gate may still be too permissive.
   - The setup-level `minecraft >= 1.21` guard exists.
   - Extra hard vetoes were added in:
     - `RendererMixinPlugin`
     - `FullSlabsMixinPlugin`
   - The next run should confirm whether those vetoes are actually firing.

3. The failing mixin may not be the one we were instrumenting.
   - The fresh logs did not expose `renderer-preApply` / `renderer-skip`.
   - That means the remaining culprit may be a different `class_776`-targeting path.

## Important file references

- `src/main/java/me/modmuss50/optifabric/util/ZipUtils.java`
- `src/main/java/me/modmuss50/optifabric/mod/OptifabricSetup.java`
- `src/main/java/me/modmuss50/optifabric/mod/OptifineSetup.java`
- `src/main/java/me/modmuss50/optifabric/mod/OptifineInjector.java`
- `src/main/java/me/modmuss50/optifabric/patcher/fixes/OptifineFixer.java`
- `src/main/java/me/modmuss50/optifabric/patcher/fixes/BlockRenderManagerFix.java`
- `src/main/java/me/modmuss50/optifabric/patcher/fixes/BakedModelManagerFix.java`
- `src/main/java/me/modmuss50/optifabric/patcher/fixes/WorldRendererFix.java`
- `src/main/java/me/modmuss50/optifabric/compat/fabricrendererapi/RendererMixinPlugin.java`
- `src/main/java/me/modmuss50/optifabric/compat/full_slabs/FullSlabsMixinPlugin.java`
- `debug/latest.log`
- `debug/optifabric-startup.log`
- `debug/optifabric-debug/net_minecraft_class_776.class`

## Workflow note

The `debug/` folder is ephemeral and must be re-read after every run. Use only the latest regenerated files when deciding the next change.
