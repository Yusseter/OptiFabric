# Debug Workflow Protocol

This repository uses a fresh `debug/` folder for each test run.

Rules:

1. Treat every `debug/` folder as ephemeral.
2. Re-read the current `debug/` contents after each run.
3. Do not assume any previous `debug/` file still applies.
4. Base conclusions only on the latest regenerated logs and artifacts.
5. After code changes, rebuild the mod jar before asking for the next run.
6. The user replaces the jar and the `debug/` folder contents between runs.

When analyzing a failure, use only the current run's files:
- `debug/latest.log`
- `debug/optifabric-startup.log`
- `debug/optifabric-debug/*.class`
- any other files regenerated for that run

If the fresh `debug/` contents conflict with earlier assumptions, trust the fresh files.
