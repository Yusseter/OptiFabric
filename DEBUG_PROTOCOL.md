# Debug Protocol

## Primary Mode

Work autonomously from the repo-local copied runtime:

```text
.local/minecraft-runtime/.minecraft
```

Build, install, run, inspect logs, capture thread dumps, and iterate locally. The user should not need to rerun Minecraft after every code change.

## Local Feedback Loop

1. Rebuild OptiFabric.
2. Copy the new `build/libs/optifabric-1.15.2.jar` into `.local/minecraft-runtime/.minecraft/mods`.
3. Run the copied Minecraft/Fabric runtime with Java 21.
4. Inspect generated logs, crash reports, `.optifine` artifacts, and class dumps.
5. If the process hangs, capture a thread dump from the same shell session before killing it.
6. Fix the next concrete failure.
7. Repeat.

## User-Provided `debug/`

`debug/` is now optional external evidence, mainly for Windows validation.

Rules:

1. Re-read `debug/` only when the user says it was updated or asks about it.
2. Treat `debug/` as a snapshot from one external run.
3. Do not use stale `debug/` files over fresh local runtime evidence.
4. If local evidence and `debug/` disagree, decide whether the difference is platform-specific before acting.

Useful files, when present:

- `debug/latest.log`
- `debug/crash-reports/*`
- `debug/optifabric-startup.log`
- `debug/optifabric-debug/*.class`
- copied `.optifine` files

## Windows Validation

Ask the user for a Windows run only after local testing has removed the current Java/classloading/mixin failure, or when a suspected bug depends on Windows rendering, audio, file paths, or launcher behavior.
