# 04 — Player renderer strategy: mixin-based body-mesh substitution

**Status:** Locked. This is the single most consequential decision in the project.

**Update log:**
- 2026-04-29 — R1 verified false by reading playerAnimator's bytecode; no priority conflict on `Model.renderToBuffer`. R5 added (GeckoLib helper-renderer ergonomics). Phase 2 acceptance milestone made explicit.

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

### R1: ~~Mixin priority conflict with playerAnimator~~ — verified non-issue (2026-04-29)

Original concern: that playerAnimator's `LivingEntityRenderRedirect_bendOnly` and our mixin would both `@Redirect` `Model.renderToBuffer`, and Mixin only allows one redirect per call site.

**Verified false by reading playerAnimator's actual bytecode** (`player-animation-lib-forge-1.0.2-rc1+1.20.jar`). PlayerAnimator's mixins on `LivingEntityRenderer.render` target two completely different call sites:

| Mixin handler | Target |
|---|---|
| `LivingEntityRenderRedirect_bendOnly.initialPush` | `@Inject(at=INVOKE)` on `List.iterator()` (start of layers loop) |
| `LivingEntityRenderRedirect_bendOnly` redirect | `@Redirect` on `Iterator.next()` inside layers loop |
| `PlayerRendererMixin.applyBodyTransforms` etc. | `@Inject` on the `super.render()` call inside `PlayerRenderer.render` |

**Nothing in playerAnimator redirects `Model.renderToBuffer`.** Our redirect on that call site is uncontested. No priority configuration needed.

### R2: SpongePowered Mixin gradle plugin + Kotlin plugin circular-dep

Phase 0 hit it; we deferred. Phase 2 has to solve it. Estimated 30–60 min.

### R3: Bone-name assumption needs cross-model verification

The six conventional names (`head`, `body`, `rightArm`, `leftArm`, `rightLeg`, `leftLeg`) are assumed across all 19 builtin models. An author who named a bone `Head` (capital H) or `arm_right` would silently no-op and leave that limb in T-pose.

**Verification step before writing the bone mirror:** extract `models/main.json` from each of the 19 builtins (Phase 4 territory), grep for top-level bone names, confirm the convention. If any model deviates, the mirror needs a per-model alias table — adds a `boneAliases: Map<String, String>` field to the registry.

This is added to Phase 4's verification checklist (see `tasks/phase-4-model-resources.md` → Verification).

### R4: Layers render in vanilla shape on top of bedrock model

After our redirect substitutes the body draw, the layers loop runs on the *vanilla* `HumanoidModel` poses. Armor, elytra, and item-in-hand will render in normal-Steve proportions on top of a possibly-smaller bedrock body. This is visually wrong but functionally correct (the player is still wearing the armor; HUD updates; damage reduction applies). See [05-armor-deferred.md](05-armor-deferred.md).

### R5: GeckoLib helper-renderer ergonomics

The plan calls for using `GeoReplacedEntityRenderer` as an internal helper inside our mixin. Its constructor signature `(EntityRendererProvider.Context, GeoModel<T>, T)` requires a valid `Context` — typically obtained during `EntityRenderersEvent.RegisterRenderers`. Driving it from inside a redirect handler bypasses its normal construction path; we'll need to either capture a Context at startup (via the entity-renderer-register event) or skip `GeoReplacedEntityRenderer` entirely and use GeckoLib's lower-level primitives directly:

- `GeckoLibCache.getBakedModels().get(location)` → `BakedGeoModel`
- `model.handleAnimations(animatable, instanceId, animationState)` to compute pose state
- Walk `bakedModel.topLevelBones()` and call `GeoRenderer.renderRecursively(...)` per bone

**Plan:** at the start of Phase 2 implementation, spend ~30 min validating which of the two paths actually compiles and runs. If `GeoReplacedEntityRenderer` works as a helper, use it. If not, fall through to the lower-level API. Either way the rest of the architecture is unchanged — this is an implementation-detail choice, not an architectural one.

## Phase 2 acceptance milestone (go/no-go)

Phase 2 is "done" when the following runs without errors in the live `落幕曲 1.6.3` modpack instance:

1. **Vanilla replacement works** — drop one builtin YSM model into the resources, hardcode it as the active model, launch the modpack. The local player renders as the bedrock model instead of Steve.
2. **Pose plumbing works** — equip a TACZ gun. Right-click to aim. The bedrock arm raises into the aim pose. (This proves the bone mirror reads playerAnimator-mutated `ModelPart` state correctly.)

If both pass, Phase 2 is locked in and Phase 3+ can build on top. If milestone 2 fails, we have a debugging session to identify whether the bone mirror's bone-name lookup, transform copying, or render-order assumptions are wrong — but the architecture itself is salvageable.

If milestone 1 fails (no model renders, stack trace, etc.), the architecture is in question and we re-evaluate.

## What does this mean later?

- **Phase 2 implementation is constrained.** No registering separate renderers, no replacing `PlayerRenderer`. The mixin + bridge layer is the only path.
- **Phase 6 (TACZ/SlashBlade compat) becomes mostly free.** Because we're letting vanilla `setupAnim` run unmodified, those mods' pose mutations flow through automatically. Phase 6 narrows to: verify TACZ aim animation triggers the right YSM extra-animation key (e.g. `tac` from the `animation` map in `ysm.json`), and similarly for SlashBlade.
- **Phase 7 (armor, first-person, polish)** picks up the cosmetic gaps the bone mirror leaves behind.
- **First-person view** uses a different render path (`ItemInHandRenderer` plus first-person-specific code), which is why Phase 7 has its own arm-rendering task.
