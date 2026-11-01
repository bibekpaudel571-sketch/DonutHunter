# DonutHunter — Minecraft 26.1.2

This repository is configured to build on GitHub Actions using Java 25, Fabric Loom 1.16.3, Fabric Loader 0.19.3 and Meteor Client 26.1.2-SNAPSHOT.

## Build on GitHub

1. Create a new empty GitHub repository named `DonutHunter`.
2. Upload the contents of this folder (not the outer folder itself) to the repository.
3. Open the repository's **Actions** tab and enable Actions if GitHub asks.
4. Open the **Build DonutHunter** workflow. It runs on every push; you can also select **Run workflow**.
5. When the run finishes successfully, open that run and download the `DonutHunter-Minecraft-26.1.2` artifact.
6. Extract the downloaded artifact ZIP and put the `.jar` in the same `mods` folder as Meteor Client.

## Current implementation status

The initial code registers three working manual scan modules:

- **Block Cluster Analyzer** — counts chests, trapped chests, barrels, shulker boxes and spawners within 32 blocks.
- **Storage Scanner** — counts nearby storage blocks within 48 blocks.
- **Spawner Scanner** — counts nearby spawners within 64 blocks.

These are bounded scans of the currently loaded client world. They report counts in chat; they are not ESP overlays and do not scan unloaded chunks. The other features discussed (Storage ESP, Spawner ESP, Block ESP, Deepslate ESP, Hole ESP, Tunnel ESP, Nether ESP, SusChunkFinder, ChunkFinder, AdvancedStashFinder, BaseFinder, RegionMap and Freecam) are not implemented in this starter build yet. They need separate implementation and testing against the 26.1.2 API.

## Compatibility note

The build configuration follows the 26.1.x unobfuscated setup: Java 25, no mappings dependency, and the `net.fabricmc.fabric-loom` plugin. This project has not been run through GitHub Actions yet, so the first CI run is the compatibility check. If it fails, share the failed Actions log and the dependency/API issue can be fixed.
