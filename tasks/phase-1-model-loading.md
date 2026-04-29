# Phase 1: Core Model Loading

## Objective
Load Bedrock-format `.geo.json` models using GeckoLib 4's API and prepare the model registry system.

## Context
YSM loads Bedrock Edition format models (`minecraft:geometry` JSON) for player models. GeckoLib 4 natively supports this format — we just need to write the loading and registry code. The original 1.1.5 source embedded a full copy of GeckoLib 3 (6,724 LOC) + MClib (1,798 LOC) to handle model parsing. GeckoLib 4 handles all of this as an external dependency.

## Tasks

### 1.1 Define model data structures
Create Kotlin data classes for model metadata:

```kotlin
// Reference: YesSteveModel/src/main/java/com/elfmcys/yesstevemodel/model/
data class YSMModelMeta(
    val spec: Int,
    val metadata: ModelMetadata,
    val files: ModelFiles
)

data class ModelMetadata(
    val name: String,
    val author: String,
    val license: License,
    val features: List<String>,
    val tips: String
)

data class ModelFiles(
    val player: PlayerModelFiles
)

data class PlayerModelFiles(
    val model: ModelReference,        // main geo.json
    val armModel: ModelReference?,    // arm geo.json (for first person)
    val animation: AnimationReference,
    val armAnimation: AnimationReference?,
    val texture: Map<String, TextureReference>  // skin variants
)
```

Reference the `ysm.json` schema from built-in models (inside ysm-2.6.2 jar at `assets/yes_steve_model/builtin/*/ysm.json`).

### 1.2 Model registry / manager
Create a `YSMModelManager` that:
- Scans `assets/yes_steve_model/builtin/` for model directories
- Parses each `ysm.json` to build a registry of available models
- Provides model lookup by ID (e.g., `wine_fox/01_taisho_maid`)
- Handles skin variant switching (multiple textures per model)

Reference: `YesSteveModel/src/main/java/com/elfmcys/yesstevemodel/client/ClientModelManager.java` (230 LOC)

### 1.3 GeckoLib 4 GeoModel integration
Create a GeckoLib 4 compatible entity/model class:
- Implement `GeoEntity` or use `AnimatableItem` pattern for the player replacement
- Load `.geo.json` via GeckoLib 4's resource caching (`GeckoLibCache`)
- Register the model with GeckoLib's asset system

Reference GeckoLib 4 API:
- `GeoEntity` interface
- `GeoModel` class
- `GeckoLibCache` for resource management

### 1.4 Model resource location helper
Utility to resolve model paths:
- Built-in models: `assets/yes_steve_model/builtin/{model_id}/models/main.json`
- Textures: `assets/yes_steve_model/builtin/{model_id}/textures/{variant}.png`

## Reference Files
- Model data structures: `/Users/neil/Code/guion-opensource/YesSteveModel/src/main/java/com/elfmcys/yesstevemodel/model/` (665 LOC, 6 files)
- Client model manager: `/Users/neil/Code/guion-opensource/YesSteveModel/src/main/java/com/elfmcys/yesstevemodel/client/ClientModelManager.java`
- GeckoLib 4 examples: https://github.com/bernie-g/geckolib (check README and examples)
- Built-in ysm.json files: Inside `ysm-2.6.2-forge+mc1.20.1-release.jar` at `assets/yes_steve_model/builtin/`

## Verification
- [ ] Can parse ysm.json from built-in models
- [ ] Model registry populates with all 19 built-in models
- [ ] Can resolve geo.json resource location for any registered model
- [ ] GeckoLib 4 can load the bedrock geo.json without errors
