# Phase 7: Polish & First-Person Support

## Objective
Handle first-person arm rendering, armor visibility, config file, and overall polish.

## Context
The original YSM has special handling for first-person view (separate arm model), armor overlay rendering, elytra rendering, and various configuration options. These are quality-of-life features that make the mod feel complete.

## Tasks

### 7.1 First-person arm rendering
When in first-person view, the player should see the custom model's arms (not vanilla Steve arms):
- Load a separate arm model (`models/arm.json`) if available in the model
- Use the arm animation (`animations/arm.animation.json`) for arm animations
- This requires hooking into Minecraft's first-person rendering pipeline

Reference:
- `/Users/neil/Code/guion-opensource/YesSteveModel/src/main/java/com/elfmcys/yesstevemodel/client/compat/FirstPersonCompat.java` (21 LOC — compatibility layer)
- `/Users/neil/Code/guion-opensource/YesSteveModel/src/main/java/com/elfmcys/yesstevemodel/client/FirstPersonCompat.java` (in client/)
- FirstPerson mod dependency: `firstperson-forge-2.2.3-mc1.20.jar` (in YesSteveModel libs/)

Note: First-person support can be deferred — the mod is fully functional without it.

### 7.2 Armor rendering
When the custom model is active, vanilla armor should still render:
- Armor pieces should overlay on top of the custom model
- GeckoLib 4's layer system should handle this via `ArmorLayer` or equivalent
- May need to adjust armor position/rotation to fit the custom model's proportions

Reference: Check GeckoLib 4 docs for armor rendering on custom entities.

### 7.3 Configuration file
Create a Forge config (`YSMConfig.kt`):
```kotlin
object YSMConfig {
    // Default model ID (empty = default Steve)
    val defaultModel: ForgeConfigSpec.ConfigValue<String>
    
    // Enable/disable for specific players
    val enabled: ForgeConfigSpec.BooleanValue
    
    // Keybind for model screen (default: Y)
    val modelScreenKey: ForgeConfigSpec.ConfigValue<String>
    
    // Keybind for animation trigger (default: Z)
    val animationKey: ForgeConfigSpec.ConfigValue<String>
}
```

Reference: `/Users/neil/Code/guion-opensource/YesSteveModel/src/main/java/com/elfmcys/yesstevemodel/config/GeneralConfig.java` (78 LOC)

### 7.4 NotEnoughAnimations compatibility
NEA (NotEnoughAnimations) is a mod that adds arm swing animations. It may conflict with our custom model:
- Test if NEA plays nicely with our renderer
- If not, add a compatibility check to disable NEA for our models

Reference: NEA is listed as a dependency in the original build.gradle.

### 7.5 Resource pack integration
Allow players to add custom models as resource packs:
- Scan resource packs for `assets/ysm/models/` directories
- Merge with built-in models in the registry
- This enables future expansion without modifying the mod jar

### 7.6 Error handling and logging
- Graceful fallback to vanilla model if a model fails to load
- Log model loading errors clearly
- Handle missing textures (use a fallback texture)
- Handle corrupt/invalid geo.json files

## Reference Files
- FirstPerson compat: `/Users/neil/Code/guion-opensource/YesSteveModel/src/main/java/com/elfmcys/yesstevemodel/client/compat/`
- Config: `/Users/neil/Code/guion-opensource/YesSteveModel/src/main/java/com/elfmcys/yesstevemodel/config/GeneralConfig.java`
- Render utilities: `/Users/neil/Code/guion-opensource/YesSteveModel/src/main/java/com/elfmcys/yesstevemodel/util/RenderUtil.java` (327 LOC)

## Verification
- [ ] First-person view shows custom model's arms (if arm model exists)
- [ ] Armor renders correctly on custom model
- [ ] Config file is created and readable
- [ ] Changing config values takes effect after restart
- [ ] Missing model gracefully falls back to vanilla
- [ ] No crash when loading invalid model data
