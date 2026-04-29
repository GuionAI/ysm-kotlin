# 05 — Armor rendering is deferred to Phase 7

**Status:** Locked

## What's the problem?

When a player wears armor in Minecraft, `LivingEntityRenderer.render` has a `layers` loop that runs *after* the body mesh is drawn. The armor layer (`HumanoidArmorLayer`) creates a separate `HumanoidArmorModel` instance, copies the pose from the player's `HumanoidModel`, and renders armor mesh on those bones.

Once we substitute the body draw with our GeckoLib bedrock model (see [04](04-player-renderer-strategy.md)), the armor layer still runs — but it draws *Steve-shaped* armor mesh on top of our possibly-smaller, differently-proportioned bedrock body.

Visually: the helmet floats above the actual character's head; the chestplate hangs in midair where Steve's body would be; the leggings and boots cluster around the waist. It looks broken.

## What did we consider?

### Option A: Fix armor in Phase 2

Cost: ~200 LOC of armor-rendering code. Need to either:
1. Reimplement `HumanoidArmorLayer` against our GeckoLib bones (use GeckoLib 4's `ItemArmorGeoLayer` which is purpose-built for this), AND
2. Encode per-model bone-to-armor-slot mapping somewhere — because each YSM model's armor bones may have different names. The current `ysm.json` schema doesn't include this mapping, so we'd also need to define a convention or extend the manifest.

Phase 2's goal is "the bedrock body renders." Adding armor doubles the surface area and introduces a per-model-config concern (which armor bone maps to helm/chest/legs/feet) that's better designed once we have the body working and can iterate.

### Option B: Hide armor entirely while a YSM model is active

Ugly but simple. Players lose the visual of their gear, which is a meaningful gameplay UX regression — armor durability and rarity feel matter visually.

### Option C: Accept the wrong-shape rendering, ship Phase 2 without addressing it

Visually wrong but mechanically complete:
- Armor still equips and unequips
- Damage reduction applies normally
- HUD armor bar updates
- Other players see your armor (in their client's vanilla shape on top of your YSM body)

The only loss is aesthetics. That's a real loss but a recoverable one — Phase 7 fixes it.

## What did we pick, and why?

**Option C.** Accept wrong-shape armor in Phases 2–6, fix in Phase 7.

Reasoning: the rule for what goes in core vs polish is **functional vs cosmetic**.

- *Functional* visual indicators are blocking. No item-in-hand = combat is broken (you can't see your weapon). No elytra = gliding has no visual feedback. These are in Phase 2.4.
- *Cosmetic* visual flaws are recoverable. Wrong-shape armor is ugly but doesn't break gameplay.

Putting armor in Phase 2 also costs us in another way: we'd be designing the armor-bone-mapping mechanism *before* we've learned anything from running the bridge layer in-game. That's a textbook premature design.

## What does this mean later?

### What Phase 2 ships with

- Bedrock body renders correctly with playerAnimator-driven poses ✓
- Item-in-hand still renders (vanilla shape, on the vanilla rightArm position — visible and usable) ✓
- Elytra still renders (vanilla shape, on the vanilla body position) ✓
- Armor still renders (vanilla shape, in approximately the right spot but visibly clipping/floating against the bedrock body) ⚠

### What Phase 7 has to deliver for armor

- A per-model armor-bone mapping (`ysm.json` schema extension or a convention by bone name)
- A custom layer that uses GeckoLib's `ItemArmorGeoLayer` to render armor on the correct bedrock bones
- Suppressing the vanilla armor layer when a YSM model is active

### Worst-case fallback

If Phase 7's per-model armor work runs into the same complexity that defeated the original YSM team, the escape hatch is Option B — hide armor entirely on YSM models — as a config flag. Worse than Option C; better than shipping with armor floating around the model permanently.
