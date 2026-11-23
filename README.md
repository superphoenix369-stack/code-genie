# Code Genie (Fabric, Minecraft 26.3)

A retro cheat cartridge for Minecraft, inspired by the NES Game Genie. See `MODRINTH.md` for the player-facing description and the full codebook. That file is ready to paste into the Modrinth project page.

## Build the jar

You need **JDK 25** (Minecraft 26.x requires Java 25).

```sh
./gradlew build
```

The mod jar is `build/libs/codegenie-1.0.0+26.3.jar`. Upload that one, not the `-sources` jar.

**No local Java?** Push this folder to a GitHub repository. The included workflow (`.github/workflows/build.yml`) builds on every push. Download the jar from the run's **codegenie-jar** artifact.

**Test it in game:** `./gradlew runClient` starts a dev client with the mod loaded.

## Publish on Modrinth

1. Create a project at modrinth.com → *Create a project* → type **Mod**.
2. Icon: `modrinth-icon-512.png`. Description: paste `MODRINTH.md`.
3. Settings: client side **Optional**, server side **Required**. Suggested categories: Game Mechanics, Utility. License: MIT.
4. Upload version: the jar above. Loader **Fabric**, game version **26.3**, version number `1.0.0+26.3`.
5. Add dependency: **Fabric API** (required).

## Layout

```
src/main/java/dev/codegenie/
  core/GenieCode.java     NES Game Genie alphabet + real decoder (pure Java)
  core/Cheat.java         the codebook (Game Genie + Classic sections)
  core/CodeMatcher.java   turns typed input into cheats / glitches / errors
  CheatEngine.java        what every code does (effects, events, warp, lives)
  Glitches.java           harmless random effects for unknown codes
  GenieCommand.java       /genie and /codegenie
  GenieConfig.java        config/codegenie.json
  Mc.java                 small helpers over vanilla APIs
  DkModePayload.java      server-to-client packet that switches DK Mode
src/client/java/dev/codegenie/client/
  CodeGenieClient.java    G keybind
  GenieScreen.java        code-entry screen (sends /genie ... or /genie cheat ...)
  BigHeads.java           DK Mode head scaling
  mixin/                  hooks into model posing for DK Mode
```

### Adding a code

Add an entry to `Cheat.java`. A Game Genie code must be 6 or 8 letters from `APZLGITYEOXUKSVN`. A classic cheat is any phrase in the `Section.CLASSIC` part. Then add its behaviour in `CheatEngine.maintain(...)` for a toggle or `CheatEngine.fire(...)` for an instant cheat.

### Porting to another Minecraft version

Bump `minecraft_version`, `loader_version`, `loom_version` and `fabric_api_version` in `gradle.properties` (current values are on https://fabricmc.net/develop). Effects, sounds and items are looked up by registry id strings (e.g. `"jump_boost"`), so most of `CheatEngine` stays the same between versions. The client screen uses the 26.x GUI API (`GuiGraphicsExtractor`, `extractRenderState`), so it would need small changes for 1.21.x.
