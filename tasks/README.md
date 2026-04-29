# YSM-Kotlin: Open Source Player Model Replacement

## Overview
An open-source reimplementation of Yes Steve Model (YSM) in Kotlin, using GeckoLib 4 as an external dependency. Targets MC 1.20.1 / Forge 47.3.22.

## Why
YSM (the original mod) blocks macOS users via hardcoded platform detection in its C++ native core. This project aims to provide the same functionality using only Kotlin/Java code with standard Bedrock model format support.

## What we're reusing from the original
The 1.1.5 reverse-engineered source at `/Users/neil/Code/guion-opensource/YesSteveModel` (17,924 LOC) serves as an **architectural reference**. We are NOT porting it line-by-line. Instead:
- ~60% of the original code (GeckoLib 3 copy + MClib + network + bukkit + encryption) is **eliminated** by using GeckoLib 4 as an external dependency
- The remaining ~40% (animation system, renderer, conditions, capability, GUI) is **rewritten in Kotlin** using GeckoLib 4's modern API

## Phase overview
| Phase | Content | LOC estimate | Priority |
|---|---|---|---|
| 0 | Project scaffolding | ~100 | Must |
| 1 | Model loading (Bedrock geo.json via GeckoLib 4) | ~400 | Must |
| 2 | Player renderer replacement | ~400 | Must |
| 3 | Animation system + conditions | ~600 | Must |
| 4 | Built-in model resources | ~0 (extraction script) | Must |
| 5 | GUI (model/skin selection) | ~400 | Must |
| 6 | PlayerAnimator compatibility (TACZ/SlashBlade) | ~300 | Critical |
| 7 | Polish (first-person, armor, config) | ~300 | Nice-to-have |
| **Total** | | **~2,500 LOC** | |

## Key dependencies
- GeckoLib 4 `software.bernie.geckolib:geckolib-forge-1.20.1:4.8.2` (MIT) — Bedrock model rendering + animation engine
- Kotlin for Forge `thedarkcolour.kotlinforforge:4.12.0` — Kotlin language support for Forge mods
- PlayerAnimator (MIT) — third-party mod dependency for TACZ/SlashBlade compat
- Maven: `https://dl.cloudsmith.io/public/geckolib/geckolib/maven/` for GeckoLib

## Built-in models (19 total)
Extracted from `ysm-2.6.2-forge+mc1.20.1-release.jar`:
- Default series: 4 models (CC 0, free)
- Wine Fox series: 15 models (mostly CC BY-NC-SA)
- 84+ skin textures total
- 98 animation files total

## Critical unknowns
- **Phase 2 (Player renderer replacement)** is the hardest phase. GeckoLib 4's `GeoEntityRenderer` is designed for custom entities, NOT for replacing the Player renderer. We likely need mixin injection into PlayerRenderer. The original YSM 1.1.5 solved this by embedding a modified GeckoLib 3 copy (6,724 LOC). Two approaches: (A) mixin into PlayerRenderer, or (B) register a full custom EntityRenderer<Player>.
- **Phase 6 (PlayerAnimator compat)** is the second-highest risk. TACZ/SlashBlade modify vanilla HumanoidModel bones via PlayerAnimator. Whether GeckoLib's custom renderer picks up those modifications automatically depends on the implementation chosen in Phase 2. If using mixins into PlayerRenderer, it should work naturally. If using a fully separate renderer, a bridge layer is needed.
- **First-person arm rendering** (Phase 7) may require FirstPerson mod or custom mixin work.

## Source code structure
```
ysm-kotlin/
├── src/main/kotlin/com/elfmcys/yesstevemodel/
│   ├── YSMMod.kt                    # Entry point
│   ├── client/
│   │   ├── YSMClientEvents.kt       # Client event bus
│   │   ├── renderer/
│   │   │   ├── YSMPlayerRenderer.kt # GeckoLib player renderer
│   │   │   └── layers/              # Armor, elytra, item layers
│   │   ├── animation/
│   │   │   ├── YSMAnimationController.kt
│   │   │   ├── AnimationManager.kt
│   │   │   └── condition/           # Hold, swing, use conditions
│   │   └── gui/
│   │       ├── YSMModelScreen.kt    # Model selection
│   │       └── SkinSelector.kt      # Skin variant cycling
│   ├── model/
│   │   ├── YSMModelMeta.kt          # Model metadata data classes
│   │   └── YSMModelManager.kt       # Model registry
│   ├── capability/
│   │   └── PlayerModelData.kt       # Persist player's model selection
│   └── config/
│       └── YSMConfig.kt             # Forge config
├── src/main/resources/
│   ├── META-INF/mods.toml
│   └── assets/ysm/models/builtin/   # 19 built-in models
└── tasks/                            # These task files
```
