# 07 — Animation architecture: YSM fidelity vs vanilla-pose compat

**Status:** Open. This doc proposes options; we pick one before more code.

## What's the problem?

Two animation sources both want to drive the bedrock player model, and neither alone is sufficient.

### Source A: YSM-authored GeckoLib animations

Each YSM model ships a `main.animation.json` with hand-crafted keyframe-based animations for every bone, including decorative ones (cape, hair, ears, fox mount, ribbons, accessories). The animations are stylized: the YSM author chose specific timing, poses, and personality. A wine_fox walk doesn't look like a vanilla Steve walk; it has fox-girl-specific arm sway.

These animations live on **all** the bones in the bedrock model. Vanilla's `HumanoidModel` only has six parts (head/body/rightArm/leftArm/rightLeg/leftLeg). YSM models can have 50+ bones. Only the YSM animation knows what to do with the cape, the hair tie, the fox ears.

### Source B: Vanilla `HumanoidModel` post-`setupAnim` state

Every frame, vanilla `LivingEntityRenderer.render` calls `HumanoidModel.setupAnim(...)` which writes pose rotations into the six top-level `ModelPart`s based on the player's live state (walking, sneaking, swimming, attacking, etc.). Then a chain of mods piles on:

- **playerAnimator (KosmX)** — its mixins fire during `setupAnim` and overlay track-based emotes/poses.
- **TACZ** — uses playerAnimator under the hood; aim/recoil poses end up in `HumanoidModel.rightArm` etc.
- **SlashBlade** — same pattern; sword stance and swing keyframes land in vanilla parts.
- **Combat Nouveau, Cataclysm, etc.** — combat mods that modify `HumanoidModel` directly.

By the time `model.renderToBuffer` runs (the call we redirect), the vanilla `HumanoidModel.ModelPart` tree contains the *fully processed* pose: vanilla movement + every mod's overlay. The trick we discovered in `decisions/04` is that this is the natural integration point — read the post-mutation state and project it onto bedrock bones.

### Why neither source alone works

| Animation | YSM-authored (Source A) | Vanilla mirror (Source B) |
|---|---|---|
| Walking | ✓ stylized YSM walk | ✓ vanilla limb cycle |
| Running | ✓ YSM run | ✓ vanilla limb cycle |
| Jumping | ✓ YSM jump pose | ✗ vanilla doesn't pose-change for jump (it's just vertical position) |
| Swimming | ✓ YSM swim | ⚠ vanilla rotates the body to horizontal — but body rotation is in `body.xRot`, which we currently can't mirror because of the nested-hierarchy problem |
| Attack swing | ✓ YSM attack | ✓ vanilla `attackTime` swings rightArm |
| Sneak body bend | ✓ YSM sneak | ⚠ same body-rotation issue |
| Cape flutter | ✓ YSM cape keyframes | ✗ vanilla doesn't have a cape bone |
| Fox mount idle bob | ✓ YSM fox bob | ✗ vanilla has no fox concept |
| Hair tie sway | ✓ YSM | ✗ no equivalent |
| TACZ aim pose | ✗ YSM doesn't know about TACZ state | ✓ playerAnimator wrote it into vanilla `rightArm` |
| SlashBlade swing | ✗ same | ✓ same |
| KosmX emote | ✗ same | ✓ same |

YSM-authored owns "anything that wasn't in vanilla" + stylization. Vanilla mirror owns "anything mods modify after vanilla setupAnim runs."

### What we tried (failed) so far

1. **GeckoLib animations only (Phase 3a)** — rich YSM look on land, but TACZ/etc. invisible because we never read vanilla pose.
2. **Bone mirror only (this attempt, Phase 6 first cut)** — lost most YSM richness because vanilla doesn't communicate jump/swim/cape/decoration via the six ModelParts. Reduced the model to "vanilla animations on a custom mesh."

The problem statement is clear: we need **both** sources to drive the bedrock model, and the architecture must decide *which one wins for which bone, when*.

## The architectural options

### Option 1 — GeckoLib base, per-mod hooks for compat

GeckoLib animations drive everything by default. For each compat-relevant mod (TACZ, SlashBlade, playerAnimator), we write code that detects when that mod is active and overrides the affected bones with the mod's intended pose.

| | |
|---|---|
| Pros | Full YSM fidelity. No surprises in the YSM look. |
| Cons | Every new mod that touches the player needs custom integration code. Tightly couples our mod to specific other mods' internals (which break across versions). Doesn't generalize. The whole reason `decisions/03-target-modpack-and-compat` is locked on the post-`setupAnim` integration point is precisely because per-mod hooks don't scale. |
| Verdict | **Reject.** The modpack has 9 player-pose mods today; real-world packs add new ones every release. |

### Option 2 — Bone mirror only, YSM extras as keybind triggers

Bone mirror drives bones every frame. YSM-authored animations are reduced to *triggered overlays* (dance, wave, etc. via Z-key) that play *only* on bones the mirror doesn't touch (cape, hair, decorations).

| | |
|---|---|
| Pros | TACZ/SlashBlade/playerAnimator compat is free. Architecture is simple. |
| Cons | YSM-authored idle/walk/run/jump/swim animations are *unused*. The model looks like vanilla animations on a custom mesh. Loses the artistic intent the YSM author put into the animation file. Decoration-only triggered animations feel half-finished. |
| Verdict | **Acceptable but mediocre.** The mod technically works but the visual quality drops to "Steve in a fox costume." |

### Option 3 — Hybrid: GeckoLib base + additive mirror delta

GeckoLib animations drive bones as the base layer. Bone mirror computes a *delta* from "what vanilla setupAnim would have produced for the player's current state, with no mod mixins firing" to "what the actual post-setupAnim HumanoidModel contains" — and applies that delta as additive rotation on top of the GeckoLib pose.

So when no mod is overriding, the delta is zero and GeckoLib's animation shows. When TACZ writes an aim pose, the delta is non-zero and it adds to (or replaces) the GeckoLib-drawn arm.

| | |
|---|---|
| Pros | Theoretically the best of both worlds. |
| Cons | Computing "what vanilla would have produced without mod mixins" requires either (a) running `setupAnim` on a second `HumanoidModel` instance with mixins disabled — not possible in mixin architecture — or (b) approximating the vanilla pose mathematically, which is fragile (vanilla setupAnim has 200 lines of conditional pose math). The delta approach has subtle blending bugs (additive rotation isn't commutative; quaternions matter). |
| Verdict | **Reject.** The "subtract vanilla baseline" step is fundamentally hard. The blending isn't actually clean. |

### Option 4 — Detection-based dispatch (proposed)

On a per-frame, **per-bone** basis, decide whether to use the GeckoLib pose or the mirror pose by detecting whether some mod is *actively driving* that vanilla bone right now.

The detection signal: **does the vanilla `HumanoidModel.ModelPart` differ from the bind-pose-plus-vanilla-setupAnim state?**

We don't need to compute "what vanilla setupAnim would have produced." We just need to know "did some non-vanilla code touch this bone." Three concrete detection strategies:

**4a. PlayerAnimator API tap.** PlayerAnimator's `IMutableModel` interface (woven into `HumanoidModel` via mixin) exposes a `getEmoteSupplier()` that returns the active animation processor. If non-null and processing, an emote is active → use mirror for that bone.

**4b. Marker comparison.** Snapshot vanilla bone state *before* `setupAnim` runs (via `@Inject(at = HEAD)` on `setupAnim`), let `setupAnim` run, then compare. If post-`setupAnim` state for a given bone differs from "vanilla-only" output, mirror it; otherwise use GeckoLib.

**4c. Bone-class registration.** Maintain a static set of "bones that should always be mirrored when a known compat mod is active" — `RightArm`, `LeftArm` for TACZ aim; arms+body for SlashBlade. Use a single yes/no detection (any mod active) to flip the whole bone class. Less granular but simpler.

| | |
|---|---|
| Pros | Per-bone correctness. YSM walk shows when no mod is interfering; TACZ aim shows when a gun is held. No per-mod custom integration — detection is generic. |
| Cons | Strategy 4a couples us to playerAnimator's internal API (acceptable since we already accept playerAnimator as a soft dep). Strategy 4b needs a `setupAnim` mixin and bone-state snapshot (some boilerplate). Strategy 4c is the least clean architecturally but the simplest to implement. |
| Verdict | **Strongly favored.** Strategy 4a is the cleanest if playerAnimator is reliably present in target installs (it is — in `落幕曲` and any pack with TACZ/SlashBlade). Strategy 4c as fallback. |

### Option 5 — Layer separation by bone identity

Without per-frame detection: simply declare that **certain bones are always mirrored and certain bones are never mirrored**, by name.

| Bone class | Source | Reason |
|---|---|---|
| `Head` | mirror | head pitch/yaw is vanilla state, no YSM author overrides it |
| `RightArm`, `LeftArm` | mirror | TACZ/SlashBlade/emotes target arms; vanilla also drives walk swing |
| `RightLeg`, `LeftLeg` | mirror | walk cycle is vanilla; YSM-authored walks rarely differ here |
| `Body`/`UpperBody` | mirror (with quaternion composition) | sneak/swim body bend |
| Decorative bones (cape, hair, hat, fox mount, ribbons) | GeckoLib | only YSM authors know how these should move |

| | |
|---|---|
| Pros | No per-frame state detection. Simple architectural rule. The result mostly aligns with what users expect: "core body animations match what other mods say, accessories follow YSM-authored animation." |
| Cons | Loses YSM stylization for walking (their authored arm-sway becomes vanilla-looking). For models that have stylized idle/walk/run keyframes on the main bones, those become invisible. |
| Verdict | **Pragmatic.** Trades some YSM artistic intent for a clean, predictable, universal-compat architecture. |

## Recommendation

**Pick Option 5 first; promote to Option 4 when needed.**

Reasoning:
- Option 5 is implementable in one session: declare which bones are "vanilla-pose driven" and which are "YSM-animation driven." Mirror the former, GeckoLib the latter. No detection logic.
- Option 5 covers the entire compat envelope from `decisions/03` (TACZ, SlashBlade, playerAnimator, combat mods) for free, because they all target the bones we'd already be mirroring.
- The cost is Option 5 doesn't show YSM-authored stylized walks. But: most YSM authors put their personality into *idle*, *attack*, *dance*, and *decorative-bone keyframes*. Walk/run cycles tend to be derivative of vanilla anyway. So the visible loss is small.
- If we later find specific models where the YSM walk *really* matters and Option 5's vanilla-look isn't acceptable, we promote to Option 4 (per-frame detection) without changing the bone-class taxonomy.

## What Option 5 looks like concretely

```
HumanoidBoneMirror.apply(humanoidModel, bakedGeoModel):
  // Mirror: always copy vanilla post-setupAnim onto these bones.
  copyPart(humanoidModel.head,     "Head")
  copyPart(humanoidModel.rightArm, "RightArm")
  copyPart(humanoidModel.leftArm,  "LeftArm")
  copyPart(humanoidModel.rightLeg, "RightLeg")
  copyPart(humanoidModel.leftLeg,  "LeftLeg")
  // Body rotation: needs quaternion composition because Head + Arms are children
  // of UpperBody in bedrock hierarchy. Phase 7 polish.

YSMPlayerAnimatable.registerControllers(controllers):
  // Decorative-bone controller: plays the YSM-authored animation but ONLY for bones
  // outside the mirror set. Implemented by either:
  //   - filtering at controller level (skip Head/RightArm/etc. in the bone walk)
  //   - or relying on bone-name matching: GeckoLib animation files reference bones
  //     by name, so an animation that doesn't have keyframes for RightArm leaves
  //     RightArm at its mirrored value naturally.
  controllers.add(AnimationController("decorative", ...) { state ->
    state.setAndContinue(YSM_IDLE_DECORATIVE)  // a stripped-down idle that only
                                                 // animates cape/hair/ribbons/etc.
    PlayState.CONTINUE
  })
```

In practice the simpler form (relying on bone-name matching) works because GeckoLib animations only set rotations for bones they reference. If we run the YSM `idle` animation but the mirror runs *after* and overwrites RightArm/etc., the cape keyframes survive untouched.

## What this leaves open

1. **Body rotation propagation** (sneak/swim body bend): bedrock hierarchy nests Head and arms under `UpperBody`, so direct copy of `body.xRot` to `UpperBody.rotX` double-rotates everything below. Needs `Head.localRot = inverse(parent_rot) * vanilla.head.rot` math. **Phase 7.**

2. **R6 sign convention for mirrored bones**: still need to nail the right axis-sign pattern empirically. Last attempt was `(xRot, -yRot, -zRot)`. May need iteration. The fact that the user said "attack still becomes left arm" means the *bone-name mapping* might also need flipping — `vanilla.rightArm → bedrock.LeftArm`. Test both.

3. **GeckoLib molang restriction** (from `decisions` chat earlier): even with Option 5, the YSM `extra*` animations (dance, wave) need GeckoLib's molang parser to accept the YSM-extension expressions. We have a rewriter; it works for keyframe-only animations, drops cape physics. Acceptable for first pass.

4. **YSM-extras keybinds**: how does a player trigger a dance? Phase 5 GUI work; out of scope for this doc.

## Decision needed from you

Pick Option 5 (clean baseline; lose some YSM walk stylization) or Option 4 (richer; needs detection logic). I'm leaning Option 5 because the simplicity is real and the visible loss is small, but I want your call before another impl pass.
