# 02 — Build stack: Kotlin 2.2.21, KFF 4.12, GeckoLib 4.8.2

**Status:** Locked

## What's the problem?

Pick concrete versions for every dependency before any code is written, because Forge mods are picky about version compatibility and fixing a wrong version mid-project means deleting the gradle cache and rebuilding (~10 minutes per attempt).

The pieces:
- **Forge** — the modding platform that hosts our mod. Pinned to `1.20.1-47.3.22` because that's what the target modpack uses (see [03](03-target-modpack-and-compat.md)).
- **Kotlin** — the language we write the mod in. Vanilla Forge expects Java; running Kotlin on top requires a "language adapter."
- **Kotlin for Forge (KFF)** — the language adapter. Bundles the Kotlin standard library and lets us write `@Mod object Foo` instead of a Java class.
- **GeckoLib** — the animation engine.

## Surprises that bit us

### Kotlin version: README says 1.9.22, reality is 2.2.21

KFF's 4.x README documents Kotlin 1.9.22. We tried that. Build failed on first compile:

```
e: Class 'kotlin.Unit' was compiled with an incompatible version of Kotlin.
The actual metadata version is 2.2.0, but the compiler version 1.9.0 can read versions up to 2.0.0.
```

KFF 4.12.0 is published-against Kotlin **2.2.21** — the README is stale. Bumping our Kotlin Gradle plugin to `2.2.21` fixed it.

**Lesson:** for KFF, ignore the README's Kotlin version and read the actual published artifact's metadata.

### GeckoLib maven path: it's `geckolib3`, not `geckolib`

The Cloudsmith URL the wiki shows is:
```
https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/
```

That `geckolib3` segment looks like it should be `geckolib` (since we're using version 4). It isn't. The repo is named `geckolib3` historically and hosts versions 3 *and* 4. The phase-0 task file in `tasks/` had this wrong; tested URL gave 404 on first build attempt.

### GeckoLib needs `mclib:20` as a side-dep on 1.20.1

For Minecraft 1.20.4 and below, GeckoLib's molang implementation pulls in `com.eliotlash.mclib:mclib:20` as a runtime dep. The 1.20.5+ branch absorbed it; ours needs it explicit.

### SpongePowered Mixin Gradle plugin breaks Kotlin builds

The wiki says to apply `id 'org.spongepowered.mixin' version '0.7.+'`. We tried it. Build crashed:

```
Circular dependency between the following tasks:
:addMixinsToJar -> :compileTestJava -> :compileTestKotlin -> :jar -> :addMixinsToJar
```

The mixin plugin and the Kotlin plugin both want to wire themselves into `jar` and `compileTest*`, and they form a cycle.

**Resolution:** the mixin plugin is only required when *we* author mixins. GeckoLib 4 ships its own mixin config inside its jar and doesn't require consumers to apply the plugin. Phase 0 doesn't author any mixins, so we removed the plugin entirely.

Phase 2 *will* author mixins (for the player renderer hook). At that point we have to solve the cycle — likely by configuring the mixin plugin to attach only to the main source set, or by disabling whichever auto-wiring step starts the loop. Estimated 30–60 minutes of fiddling.

## What did we pick

```properties
# gradle.properties
minecraft_version=1.20.1
forge_version=47.3.22
kotlin_for_forge_version=4.12.0
geckolib_version=4.8.2
```

```groovy
// build.gradle plugins
id 'org.jetbrains.kotlin.jvm' version '2.2.21'
id 'org.jetbrains.kotlin.plugin.serialization' version '2.2.21'
id 'net.minecraftforge.gradle' version '[6.0,6.2)'
// SpongePowered mixin plugin: deferred to Phase 2
```

Verified: `./gradlew build` produces a working jar. Phase 1's 8 unit tests pass.

## JDK requirement on this machine

This Mac defaults to JDK 25 via Homebrew; ForgeGradle 6 only works on **JDK 17**. Every gradle invocation needs:
```
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
export PATH="$JAVA_HOME/bin:$PATH"
```

## What does this mean later?

- Bumping Kotlin past 2.2.x is fine *if* KFF ships a build using that Kotlin. Don't bump Kotlin independently of KFF.
- Bumping Forge means re-checking GeckoLib's available versions for that MC version (their version map is `geckolib-forge-<mcversion>:<libversion>`).
- Phase 2 will reintroduce the mixin plugin and we'll have to solve the circular-dep then.
- All these gotchas live in `~/.claude/projects/.../memory/phase0_stack.md` so future sessions don't relearn them.
