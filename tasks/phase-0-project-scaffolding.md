# Phase 0: Project Scaffolding

## Objective
Set up a Forge mod project in Kotlin targeting MC 1.20.1, with GeckoLib 4 as an external dependency.

## Context
We are re-implementing YSM (Yes Steve Model) — a mod that replaces the player model with custom Bedrock-format 3D models. The original 1.1.5 source is at `/Users/neil/Code/guion-opensource/YesSteveModel` (17,924 LOC, Forge 1.20, GeckoLib 3 embedded). We are writing fresh Kotlin code using GeckoLib 4 as an external dependency, which eliminates ~60% of the original codebase.

## Target Stack
- Minecraft: 1.20.1
- Forge: 47.3.22
- Language: Kotlin (via Kotlin for Forge)
- GeckoLib: 4.x (latest for 1.20.1)
- FirstPerson: 2.2.3-mc1.20 (optional, for first-person model compat)
- NotEnoughAnimations: 1.6.4-mc1.20 (optional, for arm animations)
- Java: 17

## Tasks

### 0.1 Create Forge mod project structure
```
ysm-kotlin/
├── build.gradle              # Forge + Kotlin + GeckoLib 4 dependencies
├── settings.gradle
├── gradle.properties
├── gradle/                   # Gradle wrapper
├── src/main/
│   ├── kotlin/
│   │   └── com/elfmcys/yesstevemodel/
│   │       ├── YSMMod.kt              # @Mod entry point
│   │       ├── client/
│   │       │   ├── YSMClientEvents.kt # Client-side event registration
│   │       │   ├── renderer/
│   │       │   ├── animation/
│   │       │   └── gui/
│   │       ├── model/
│   │       ├── capability/
│   │       └── config/
│   └── resources/
│       ├── META-INF/mods.toml
│       └── assets/ysm/       # Built-in model resources (Phase 4)
└── libs/                     # Optional local jar deps
```

### 0.2 build.gradle configuration
- Forge 1.20.1-47.3.22
- Kotlin plugin: `org.jetbrains.kotlin.jvm`
- Kotlin for Forge: `thedarkcolour.kotlinforforge:4.12.0` (supports 1.19.3-1.20.4)
- GeckoLib 4: `software.bernie.geckolib:geckolib-forge-1.20.1:4.8.2` (latest stable for 1.20.1 as of Sep 2025)
- Use `fg.deobf()` for GeckoLib dependency
- Maven repos needed: `maven { url 'https://dl.cloudsmith.io/public/geckolib/geckolib/maven/' }` for GeckoLib, `maven { url 'https://maven.minecraftforge.net/' }` for Forge

### 0.3 mods.toml
- Mod ID: `yesstevemodel`
- Display name: `Yes Steve Model (Open)`
- Dependencies: minecraft 1.20.1, forge 47.3.0+, geckolib 4.4+
- Version: `2.0.0-open`
- Language loader: `kotlinforforge` (not `javafml`)

### 0.4 Verify build
- `./gradlew build` should succeed
- `./gradlew runClient` should launch Minecraft with the mod loaded (empty mod, no functionality yet)

## Reference Files
- Original build.gradle: `/Users/neil/Code/guion-opensource/YesSteveModel/build.gradle`
- Original mods.toml: Check `src/main/resources/META-INF/` in YesSteveModel
- Kotlin for Forge: https://github.com/thedarkcolour/KotlinForForge
- GeckoLib 4 Forge: https://github.com/bernie-g/geckolib
- GeckoLib 4 Installation: https://github.com/bernie-g/geckolib/wiki/Installation-(Geckolib4)
- GeckoLib 4 Examples: https://github.com/bernie-g/geckolib-examples

## Errata: corrections discovered during implementation

The following points in this task file were wrong or incomplete. The actual working build (committed `feat(phase-0)` + `fix(phase-0)`) reflects the corrections; full reasoning lives in [`decisions/02-build-stack.md`](../decisions/02-build-stack.md):

- **Kotlin Gradle plugin: 2.2.21, not 1.9.22.** KFF 4.12 ships kotlin-stdlib 2.2.21 (metadata 2.2.0); the 1.9.22 plugin can't read it.
- **GeckoLib maven URL is `geckolib3/geckolib/maven/`, not `geckolib/geckolib/maven/`.** The Cloudsmith repo path uses `geckolib3` historically and hosts both v3 and v4 jars.
- **`com.eliotlash.mclib:mclib:20` is a required side-dep on 1.20.1**, even though the wiki phrases it as optional.
- **Do NOT add the `org.spongepowered.mixin` gradle plugin in Phase 0.** It deadlocks the kotlin plugin with a circular `addMixinsToJar → compileTestKotlin → jar` task graph. Phase 2 will solve this when it actually authors mixins. GeckoLib 4 ships its own mixin config inside its jar; consumers don't need the plugin to use GeckoLib.
- **JDK 17 is required** (this Mac defaults to 25 via Homebrew). Set `JAVA_HOME=/opt/homebrew/opt/openjdk@17` for every gradle invocation.

## Verification
- [ ] `./gradlew build` succeeds with no errors
- [ ] Mod appears in Forge mod list when launched
- [ ] Kotlin runtime is loaded (print log in @Mod init)
