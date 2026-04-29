# 04 — Player renderer strategy: mixin-based body-mesh substitution

**Status:** Locked. This is the single most consequential decision in the project.

## What's the problem?

Replace the visual appearance of the Minecraft player with custom 3D models, *without* breaking any of the other mods in the target modpack that animate the player.

To explain why this is hard, here's how Minecraft renders the player. Skim if you already know:

### The vanilla render pipeline (1.20.1)

Each frame, for each visible player:

1. The `EntityRenderDispatcher` looks up the renderer registered for the `Player` entity class — it's `PlayerRenderer`.
2. `PlayerRenderer.render(player, yaw, partialTick, poseStack, bufferSource, packedLight)` is called.
3. `PlayerRenderer.render` sets some visibility flags (e.g. `model.head.visible = !invisible`) then delegates to its parent, `LivingEntityRenderer.render`.
4. `LivingEntityRenderer.render` does:
   - `this.model.prepareMobModel(...)` and `this.model.setupAnim(...)` — these mutate the `ModelPart` tree that hangs off the `HumanoidModel`. After these calls, `model.head.xRot/yRot/zRot`, `model.rightArm.*`, etc. hold the *current frame's* pose.
   - `this.model.renderToBuffer(poseStack, vertexConsumer, packedLight, ...)` — this walks the `ModelPart` tree and emits triangles into the OpenGL buffer. **This is where Steve's body mesh is actually drawn.**
   - A loop over `this.layers`: armor, elytra, item-in-hand, name plate, etc. Each layer reads the `ModelPart` poses and draws on top.

The two crucial calls:
- `setupAnim` — *computes* the pose
- `renderToBuffer` — *draws* the mesh using the pose

Other mods overwhelmingly hook into `setupAnim` (or earlier) to *modify the pose*. They don't replace `renderToBuffer`.

### What other mods do (verified from 落幕曲 1.6.3)

We extracted `player-animation-lib-forge-1.0.2-rc1+1.20.jar` (KosmX's playerAnimator, on which TACZ and SlashBlade depend) and read its mixin config. It declares mixins on:

| Class targeted | What the mixin does |
|---|---|
| `HumanoidModel` (`BipedEntityModelMixin`) | Adds bend support and an animation supplier to *every* HumanoidModel instance |
| `PlayerModel` (`PlayerModelMixin`) | Adds emote support to PlayerModel specifically |
| `PlayerRenderer` (`PlayerRendererMixin`) | Applies body transforms during render, hides bones in first-person |
| `LivingEntityRenderer` (`LivingEntityRenderRedirect_bendOnly`) | Applies bend transforms on the `model.renderToBuffer` call |
| `ModelPart` | Marks "upper" parts for animation routing |
| Plus first-person-specific mixins | Camera and item-in-hand handling |

**These mixins are bound to vanilla classes by name.** If we register a different class as the player renderer, `PlayerRendererMixin` simply doesn't fire. If we don't instantiate a `PlayerModel`, `PlayerModelMixin` doesn't fire.

TACZ and SlashBlade then use playerAnimator's API to inject keyframes into the HumanoidModel during `setupAnim`. So the pose-mutation chain is:

```
LivingEntityRenderer.render  (vanilla)
  → HumanoidModel.setupAnim  (vanilla, but BipedEntityModelMixin is woven into it)
    → playerAnimator's keyframes apply  (mutates ModelPart rotations)
      → TACZ aim pose layered on top  (mutates further)
        → SlashBlade stance layered on top
          → final ModelPart state
  → HumanoidModel.renderToBuffer  (draws Steve's mesh)
  → layers (armor, elytra, item-in-hand, ...)
```

Our job is to swap the **mesh draw** while leaving the **pose computation** completely untouched.

## What did we consider?

### Option A: Register a separate `EntityRenderer<Player>` via `EntityRenderersEvent.RegisterRenderers`

**Reject.** PlayerAnimator's `PlayerRendererMixin` and `PlayerModelMixin` are class-bound to the vanilla types. Our class wouldn't trigger them. TACZ aim animations, SlashBlade swings, and emotes would all silently break. The original YSM 1.1.5 used this approach, then had to write a 6,700-LOC GeckoLib fork to compensate.

### Option B: Use GeckoLib 4's built-in `GeoReplacedEntityRenderer`

Looked promising — its name suggests "replace another entity's renderer." But its bytecode (we extracted and inspected `geckolib-forge-1.20.1-4.8.2.jar`) shows it extends `EntityRenderer<E>` directly, not `LivingEntityRenderer`. It skips `setupAnim` entirely; same problem as Option A.

We *do* still use `GeoReplacedEntityRenderer` — but as an internal helper instantiated inside our mixin, not as the registered renderer. We call its `actuallyRender(...)` method to draw the GeoModel; we don't let it own the render lifecycle.

### Option C: Mixin into `PlayerRenderer.render` and cancel at HEAD

Cancelling the entire render at HEAD is too aggressive. Vanilla `setupAnim` would be skipped, so playerAnimator and friends never run their pose mutations. We'd be back to Option A's problem — just laundered through a mixin.

### Option D: Mixin a `@Redirect` on the *single specific call* `Model.renderToBuffer` inside `LivingEntityRenderer.render`

This is the choice. Explanation below.

## What did we pick, and why?

**Mixin into `LivingEntityRenderer` with a `@Redirect` on the `Model.renderToBuffer(...)` call.**

When the redirect fires:
1. *Everything before* `renderToBuffer` in `LivingEntityRenderer.render` has already run. That means `setupAnim` ran, playerAnimator's mixins ran, TACZ and SlashBlade keyframes have already mutated the `ModelPart` tree.
2. We check whether the entity is a player with a YSM model selected. If yes, we draw our GeckoLib model instead — using the already-computed `ModelPart` poses as input via a "bone mirror" (described below). If no, we fall through to the original `renderToBuffer` call so vanilla rendering proceeds normally.
3. *Everything after* `renderToBuffer` — armor, elytra, items, name plate — runs normally on top.

The mixin sketch:

```java
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Redirect(
        method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/Model;renderToBuffer(...)V")
    )
    private void ysm$replaceBodyDraw(
        Model model, PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay,
        float r, float g, float b, float a,
        // Outer-method args captured by trailing position:
        LivingEntity entity, float entityYaw, float partialTick,
        PoseStack outerStack, MultiBufferSource bufferSource, int outerLight
    ) {
        if (entity instanceof AbstractClientPlayer p && YSMRenderBridge.shouldReplace(p)) {
            YSMRenderBridge.renderGeo(p, model, partialTick, poseStack, bufferSource, packedLight);
        } else {
            model.renderToBuffer(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        }
    }
}
```

Mixin's `@Redirect` allows trailing capture of the outer method's arguments — which is how we get `partialTick` and `entity` for free without `MixinExtras`.

### The "bone mirror" — how poses flow into the GeckoLib model

GeckoLib's `BakedGeoModel` is a tree of `GeoBone` objects. Vanilla's `HumanoidModel` is a tree of `ModelPart` objects. To draw the GeoModel using vanilla's pose state, we walk the GeoModel's bones and copy rotation/translation from the matching ModelPart:

| Vanilla `ModelPart` field | Standard Bedrock bone name |
|---|---|
| `model.head` | `head` |
| `model.body` | `body` |
| `model.rightArm` | `rightArm` |
| `model.leftArm` | `leftArm` |
| `model.rightLeg` | `rightLeg` |
| `model.leftLeg` | `leftLeg` |

The 19 builtin YSM models all use these conventional names (same author group). Per-model bone-name aliases can be added later if needed.

Anything *more* than these six bones (custom ears, tails, decorative bones) is not driven by the mirror — it's driven by GeckoLib's animation engine reading the model's own `.animation.json` files. The mirror seeds the baseline pose; GeckoLib animations layer additively on top.

## Risks and unknowns

### R1: Mixin priority conflict with playerAnimator

PlayerAnimator's `LivingEntityRenderRedirect_bendOnly` redirects on the same `Model.renderToBuffer` call. Mixin allows only **one** `@Redirect` per call site. Whichever has higher `@Mixin(priority = ...)` wins; the other is silently dropped.

**Plan:** start with our mixin at `priority = 800` (lower than the default 1000) so playerAnimator wins on conflict and our redirect doesn't apply. If that means our redirect doesn't fire at all (which it would, if priority works the way I read it), switch to `@Inject(at = "INVOKE", target = ..., cancellable = true)` — multiple `@Inject`s coexist freely.

This is the **only architecturally significant unknown** that needs in-game verification, not API research.

### R2: SpongePowered Mixin gradle plugin + Kotlin plugin circular-dep

Phase 0 hit it; we deferred. Phase 2 has to solve it. Estimated 30–60 min.

### R3: Bone-name assumption

The six conventional names are assumed across all builtin models. An author who names a bone `Head` (capital H) or `arm_right` would silently no-op and leave that limb in T-pose. Verifiable by inspecting `models/main.json` of any one builtin before writing code.

### R4: Layers render in vanilla shape on top of bedrock model

After our redirect substitutes the body draw, the layers loop runs on the *vanilla* `HumanoidModel` poses. Armor, elytra, and item-in-hand will render in normal-Steve proportions on top of a possibly-smaller bedrock body. This is visually wrong but functionally correct (the player is still wearing the armor; HUD updates; damage reduction applies). See [05-armor-deferred.md](05-armor-deferred.md).

## What does this mean later?

- **Phase 2 implementation is constrained.** No registering separate renderers, no replacing `PlayerRenderer`. The mixin + bridge layer is the only path.
- **Phase 6 (TACZ/SlashBlade compat) becomes mostly free.** Because we're letting vanilla `setupAnim` run unmodified, those mods' pose mutations flow through automatically. Phase 6 narrows to: verify TACZ aim animation triggers the right YSM extra-animation key (e.g. `tac` from the `animation` map in `ysm.json`), and similarly for SlashBlade.
- **Phase 7 (armor, first-person, polish)** picks up the cosmetic gaps the bone mirror leaves behind.
- **First-person view** uses a different render path (`ItemInHandRenderer` plus first-person-specific code), which is why Phase 7 has its own arm-rendering task.
