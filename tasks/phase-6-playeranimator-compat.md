# Phase 6: PlayerAnimator Compatibility

## Objective
Ensure that PlayerAnimator-based mods (TACZ, SlashBlade, Better Combat) can drive animations on our custom player model.

## Context
This is the **most uncertain phase** and may require debugging. TACZ and SlashBlade use PlayerAnimator (by KosmX) to play third-person player animations by modifying vanilla HumanoidModel bone rotations. Our custom GeckoLib renderer needs to either:

1. **Best case**: GeckoLib's renderer naturally reads vanilla bone rotations (similar to how YSM 1.1.5 does it) — in which case PlayerAnimator animations just work
2. **Worst case**: We need a bridge layer to read vanilla bone rotations and sync them to our GeckoLib model

There is a known issue (#136 on PlayerAnimator repo) about pivot reference problems with non-vanilla models. This affects both Figura and CPM equally, so it's a PlayerAnimator bug, not ours.

## Tasks

### 6.1 Test basic PlayerAnimator compatibility
Before writing any bridge code, test if GeckoLib 4's player replacement renderer naturally picks up PlayerAnimator bone modifications:
1. Install our mod + TACZ + PlayerAnimator
2. Hold a TACZ gun
3. Check if the custom model's arms move into gun-holding pose
4. If YES → minimal work needed (just fix any pivot issues)
5. If NO → implement bridge layer (6.2)

### 6.2 Bridge layer (if needed)
If GeckoLib doesn't naturally sync vanilla bone rotations:

```kotlin
// Read vanilla HumanoidModel bone rotations set by PlayerAnimator
// Apply them to our GeckoLib model's corresponding bones
fun syncVanillaBones(player: Player, geoModel: GeoModel) {
    val vanillaModel = ... // access vanilla HumanoidModel
    // Map vanilla bones to GeckoLib bones:
    // vanilla.head -> gecko "Head"
    // vanilla.body -> gecko "Body"  
    // vanilla.rightArm -> gecko "RightArm"
    // vanilla.leftArm -> gecko "LeftArm"
    // vanilla.rightLeg -> gecko "RightLeg"
    // vanilla.leftLeg -> gecko "LeftLeg"
}
```

Reference the condition system for how YSM detects held items:
- `/Users/neil/Code/guion-opensource/YesSteveModel/src/main/java/com/elfmcys/yesstevemodel/client/animation/condition/ConditionalHold.java`
- `/Users/neil/Code/guion-opensource/YesSteveModel/src/main/java/com/elfmcys/yesstevemodel/client/animation/condition/ConditionalSwing.java`

### 6.3 Pivot reference correction
If PlayerAnimator animations play but the pivot points are off (arms rotating around wrong axis):
- This is likely the issue described in PlayerAnimator #136
- May need to apply rotation offset corrections in the renderer
- Test with: TACZ guns (aiming/shooting), SlashBlade sword swings

### 6.4 TACZ-specific testing
TACZ has its own PlayerAnimator animation layers:
- LOWER_ANIMATION (priority 93): legs — idle/walk/run/crouch/prone
- LOOP_UPPER_ANIMATION (priority 94): arms — hold/aim/crouch/prone
- ONCE_UPPER_ANIMATION (priority 95): arms — shoot/reload/melee
- ROTATION_ANIMATION (priority 96): body rotation toward aim direction

Verify each works correctly on our custom model.

### 6.5 SlashBlade-specific testing
SlashBlade uses PlayerAnimationAPI with VMD animations:
- COMBO_A1~A5: Basic slash combos
- JUDGEMENT_CUT: Dimensional cut
- UPPERSLASH: Launch attack
- RAPID_SLASH: Rush slash

Verify animations play correctly on our custom model.

## Reference Files
- PlayerAnimator issue #136: https://github.com/KosmX/minecraftPlayerAnimator/issues/136
- TACZ animation system: https://deepwiki.com/MCModderAnchor/TACZ/4.3-player-animations
- SlashBlade animations: https://deepwiki.com/0999312/SlashBlade_Resharped/4.3-player-animations

## Verification
- [ ] TACZ: Holding a gun → model's arms in correct gun-hold pose
- [ ] TACZ: Aiming (ADS) → model's arms in aiming pose
- [ ] TACZ: Shooting → model's arms play shooting animation
- [ ] TACZ: Reloading → model's arms play reload animation
- [ ] SlashBlade: Holding sword → model in sword-hold pose
- [ ] SlashBlade: Slashing → model plays combo animation
- [ ] No visual glitches (arms spinning, pivot offset, etc.)
- [ ] When PlayerAnimator animation ends, model returns to normal idle/walk
