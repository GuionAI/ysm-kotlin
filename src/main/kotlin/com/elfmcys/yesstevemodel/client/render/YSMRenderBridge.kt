package com.elfmcys.yesstevemodel.client.render

import com.elfmcys.yesstevemodel.YSMMod
import com.elfmcys.yesstevemodel.model.RegisteredModel
import com.elfmcys.yesstevemodel.model.YSMModelManager
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.model.HumanoidModel
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.world.entity.LivingEntity
import software.bernie.geckolib.core.animation.AnimationState

/**
 * The single object the [LivingEntityRendererMixin] talks to. Owns the active-model state and
 * the actual render call.
 *
 * Phase 2: [activeModel] is hardcoded to the first builtin discovered, set once after
 * resource reload. Phase 5 will swap this for per-player capability storage driven by the
 * GUI selector.
 */
object YSMRenderBridge {
    @Volatile
    var activeModel: RegisteredModel? = null
        private set

    /**
     * The player currently being rendered, set immediately before [handleAnimations] runs.
     * The animation controller's state handler reads this to decide which animation key
     * to play (sneak vs walk vs fly etc.). Null between frames or for non-player entities.
     *
     * Render thread is single-threaded so this volatile is safe; we still null it out at
     * the end of [renderGeo] so accidental cross-frame leaks are loud.
     */
    @Volatile
    internal var currentPlayer: AbstractClientPlayer? = null
        private set

    fun shouldReplace(@Suppress("UNUSED_PARAMETER") player: AbstractClientPlayer): Boolean = activeModel != null

    fun onModelsReloaded() {
        // Phase-3 hardcoded selection: default_boy is a non-fox humanoid that ships both
        // `swim` and `swim_stand` keys. Easier to read animation transitions on than the
        // wine_fox model (which has a fox mount + lots of decorative bones).
        // Phase 5 (GUI selector) replaces this with per-player capability storage.
        val preferred = "default_boy"
        activeModel = YSMModelManager.get(preferred) ?: YSMModelManager.all.firstOrNull()
        activeModel?.let { YSMMod.LOGGER.info("YSM active model = {}", it.id) }
    }

    /**
     * Called by the mixin in place of `vanillaModel.renderToBuffer(...)`.
     *
     * `vanillaModel` carries the post-`setupAnim` pose state (vanilla + playerAnimator + TACZ +
     * SlashBlade). We copy those poses onto the GeckoLib bones, then ask GeckoLib to draw.
     */
    fun renderGeo(
        player: AbstractClientPlayer,
        @Suppress("UNUSED_PARAMETER") vanillaModel: HumanoidModel<out LivingEntity>,
        partialTick: Float,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        packedLight: Int,
    ) {
        val animatable = YSMPlayerAnimatable
        val geoModel = YSMPlayerGeoRenderer.geoModel
        // Resolve the model up front so we can bail cleanly if the cache hasn't loaded it.
        if (geoModel.getBakedModel(geoModel.getModelResource(animatable)) == null) return

        // 1. Animation handling.
        //
        // Phase 3 path: GeckoLib's animation engine drives bone poses by playing an
        // animation keyed by the controller (idle/walk/run/sneak/etc.). The previous
        // bone-mirror approach (copying vanilla HumanoidModel.ModelPart rotations onto
        // bedrock GeoBones) is disabled — it had cosmetic axis bugs (R6 in decisions/04)
        // and its primary value (TACZ/SlashBlade compat) is recovered later in Phase 6.
        //
        // limbSwing/limbSwingAmount come from the player's WalkAnimationState; isMoving
        // = limbSwingAmount > tiny threshold. The controller reads the player from
        // [currentPlayer] (stashed below) to decide which key to play.
        currentPlayer = player
        try {
            val instanceId = player.id.toLong()
            val limbSwing = player.walkAnimation.position(partialTick)
            val limbSwingAmount = player.walkAnimation.speed(partialTick)
            // Use horizontal velocity rather than walkAnimation.speed for the moving check:
            // walkAnimation.speed decays asymptotically and stays > 0 long after the player
            // has actually stopped, leaving the controller stuck on WALK. Velocity goes to 0
            // immediately when the player releases movement keys.
            val velSqr = player.deltaMovement.horizontalDistanceSqr()
            val isMoving = velSqr > 0.001 // ~0.03 blocks/tick, below normal walk speed
            val animationState = AnimationState<YSMPlayerAnimatable>(
                animatable, limbSwing, limbSwingAmount, partialTick, isMoving
            )
            geoModel.handleAnimations(animatable, instanceId, animationState)
        } finally {
            currentPlayer = null
        }

        // 2. Render.
        //
        // Vanilla LivingEntityRenderer applied scale(-1,-1,1) + translate(0,-1.5,0) before
        // model.renderToBuffer (the call we redirect). That's right for vanilla
        // HumanoidModel (which is X+Y flipped relative to world). For Blockbench-authored
        // bedrock models, the canonical setup is scale(-1, 1, 1) + translate(0,-1.5,0)
        // (X-flip only, no Y-flip). The delta we apply: a Y reflection (`scale(1,-1,1)`)
        // that un-does the Y-flip but does NOT touch Z — earlier we used Rx(180) here,
        // which inadvertently flipped Z too and ended up rendering the model facing the
        // wrong direction (W press would visually look like the character walking
        // backwards in third-person view). After the reflection we re-translate -1.5 to
        // restore foot-on-ground (the original translate was applied in the now-undone
        // flipped frame).
        //
        // Net effect: local (x,y,z) -> world (-x, y, z) — X mirror only.
        poseStack.pushPose()
        poseStack.scale(1f, -1f, 1f)
        poseStack.translate(0f, -1.5f, 0f)
        YSMPlayerGeoRenderer.defaultRender(
            poseStack, animatable, bufferSource, /*renderType*/ null, /*buffer*/ null,
            0f, partialTick, packedLight
        )
        poseStack.popPose()
    }

    // Exposed only so the model can be looked up by other phases; null-able.
    val activeModelResourcesOrNull: RegisteredModel? get() = activeModel

    // Internal accessor for the renderer's geo model — keeps the singleton private elsewhere.
    private val YSMPlayerGeoRenderer.geoModel: YSMPlayerGeoModel
        get() = this.getGeoModel() as YSMPlayerGeoModel
}
