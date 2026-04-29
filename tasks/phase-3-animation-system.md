# Phase 3: Animation System

## Objective
Implement the animation state machine that drives model animations based on player state (idle, walk, run, sneak, swim, fly, fall, etc.).

## Context
YSM uses GeckoLib's AnimationController to manage animation states. The controller receives Molang variable inputs (player speed, sneaking, swimming, etc.) and transitions between animation states. The original 1.1.5 source has ~1,087 LOC in the animation system. GeckoLib 4 handles animation playback — we need to write the state machine logic and condition checks.

## Tasks

### 3.1 Animation state machine
Create `YSMAnimationController` using GeckoLib 4's `AnimationController`:
- Define states: idle, walk, run, sneak, swim, fly, fall, sleep, elytra_fly, attack, use_item
- Use GeckoLib 4's `AnimationState` enum for state transitions
- Each state maps to an animation in the model's `animation.json`

The state machine should respond to:
- Player movement speed (walk → run threshold)
- Player pose (sneaking, swimming, sleeping, fall flying)
- Grounded state (on ground vs in air)
- Attack/use animations (triggered, then return to movement state)

Reference: `/Users/neil/Code/guion-opensource/YesSteveModel/src/main/java/com/elfmcys/yesstevemodel/client/animation/AnimationManager.java` (253 LOC)
Reference: `/Users/neil/Code/guion-opensource/YesSteveModel/src/main/java/com/elfmcys/yesstevemodel/client/animation/AnimationRegister.java` (301 LOC)

### 3.2 Condition system for held items
Implement `ConditionManager` that checks what the player is holding and triggers appropriate animations:

```kotlin
// When player holds a bow → bow_hold animation
// When player holds a crossbow → crossbow_hold animation  
// When player holds a trident → trident_hold animation
// When player holds a shield → shield_hold animation
// When player holds a fishing rod → fishing animation
```

Reference:
- `/Users/neil/Code/guion-opensource/YesSteveModel/src/main/java/com/elfmcys/yesstevemodel/client/animation/condition/ConditionManager.java` (72 LOC)
- `/Users/neil/Code/guion-opensource/YesSteveModel/src/main/java/com/elfmcys/yesstevemodel/client/animation/condition/ConditionalHold.java` (89 LOC)
- `/Users/neil/Code/guion-opensource/YesSteveModel/src/main/java/com/elfmcys/yesstevemodel/client/animation/condition/ConditionalSwing.java` (79 LOC)
- `/Users/neil/Code/guion-opensource/YesSteveModel/src/main/java/com/elfmcys/yesstevemodel/client/animation/condition/ConditionalUse.java` (118 LOC)
- `/Users/neil/Code/guion-opensource/YesSteveModel/src/main/java/com/elfmcys/yesstevemodel/client/animation/condition/ConditionArmor.java` (127 LOC)

### 3.3 Animation priority system
Animations should have priorities so higher-priority animations override lower ones:
- Attack/use animations (highest priority, momentary)
- Special hold animations (bow, crossbow, shield)
- Movement animations (walk, run, sneak)
- Idle animation (lowest priority)

Reference: `/Users/neil/Code/guion-opensource/YesSteveModel/src/main/java/com/elfmcys/yesstevemodel/client/animation/Priority.java`

### 3.4 Extra animations (expressions)
YSM models have "extra" animations like dance, wave, clap, YES, NO. These should be triggered via keybinds:
- Z key → opens animation wheel (or simplified: direct keybind per animation)
- These are defined in `extra.animation.json` within each model

### 3.5 GeckoLib 4 Molang variable integration
GeckoLib 4 has built-in Molang variable support. We need to register custom variables that the Bedrock animation controllers reference:
- `query.modified_move_speed` → player movement speed
- `query.is_sneaking` → player sneaking state
- `query.is_swimming` → player swimming state
- `query.is_on_ground` → grounded state
- `query.is_flying` → elytra flying state

## Reference Files
- Animation manager: `/Users/neil/Code/guion-opensource/YesSteveModel/src/main/java/com/elfmcys/yesstevemodel/client/animation/AnimationManager.java`
- Animation register: `/Users/neil/Code/guion-opensource/YesSteveModel/src/main/java/com/elfmcys/yesstevemodel/client/animation/AnimationRegister.java`
- Animation state: `/Users/neil/Code/guion-opensource/YesSteveModel/src/main/java/com/elfmcys/yesstevemodel/client/animation/AnimationState.java`
- All condition files: `/Users/neil/Code/guion-opensource/YesSteveModel/src/main/java/com/elfmcys/yesstevemodel/client/animation/condition/`
- GeckoLib 4 AnimationController docs: https://github.com/bernie-g/geckolib

## Verification
- [ ] Player model plays idle animation when standing still
- [ ] Player model plays walk animation when moving
- [ ] Player model plays run animation when sprinting
- [ ] Player model plays sneak animation when crouching
- [ ] Player model plays swim animation when swimming
- [ ] Holding a bow triggers bow_hold animation
- [ ] Attack key triggers attack animation briefly, then returns to movement state
- [ ] Extra animations can be triggered via keybind
