# OptiFabric 1.15.3 for Minecraft 1.21.11

Windows-tested bundle for:

- Minecraft `1.21.11`
- Java `21`
- Fabric Loader `0.17.3`
- Fabric API `0.141.4+1.21.11`
- OptiFine `OptiFine_1.21.11_HD_U_J9.jar`

Install these from this folder:

- `optifabric-1.15.3.jar`
- `yet_another_config_lib_v3-3.8.2+1.21.11-fabric-optifabric-patched.jar`
- `custom_hud-4.1.3+1.21.11-optifabric-patched.jar`
- `modmenu-17.0.0.jar`

Also keep these in the Minecraft `mods` folder:

- `OptiFine_1.21.11_HD_U_J9.jar`
- `fabric-api-0.141.4+1.21.11.jar`

Do not use the original YACL or CustomHud jars with this bundle.

Patch notes:

- OptiFabric metadata is marked as `1.15.3`.
- YACL patch fixes a malformed access widener header in the upstream jar.
- CustomHud patch relaxes stale 1.21.9 mixin anchors and removes a removed `World.getMoonSize` call.

Checksums are in `SHA256SUMS.txt`.
