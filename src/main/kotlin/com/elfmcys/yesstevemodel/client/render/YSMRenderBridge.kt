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
        vanillaModel: HumanoidModel<out LivingEntity>,
        partialTick: Float,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        packedLight: Int,
    ) {
        val animatable = YSMPlayerAnimatable
        val geoModel = YSMPlayerGeoRenderer.geoModel
        val baked = geoModel.getBakedModel(geoModel.getModelResource(animatable)) ?: return

        // 1. Animation handling.
        //
        // Phase 6 architecture: GeckoLib's controllers do NOT drive movement animations.
        // The animation pipeline is two-stage:
        //
        //   a) Stage 1 (this call): GeckoLib processes any registered controllers,
        //      ticking molang queries and applying their poses to bones. Currently
        //      registerControllers is empty so this is a no-op for movement, but it
        //      will host YSM extras (dance/wave) in Phase 5+.
        //
        //   b) Stage 2 (HumanoidBoneMirror.apply): copies the post-setupAnim
        //      HumanoidModel.ModelPart rotations onto matching bedrock bones. Because
        //      vanilla LivingEntityRenderer already called setupAnim before our
        //      redirect fired, that ModelPart state already includes:
        //        - vanilla idle/walk/run/sneak/swim/jump pose
        //        - playerAnimator track-based emote poses
        //        - TACZ aim / SlashBlade swing pose mutations
        //      All of which propagate to the bedrock model for free.
        //
        // Mirror runs AFTER the GeckoLib pass so it has the final word on the 5 mirrored
        // bones. Decorative bones (cape, hat, ribbons, ears) are untouched by the mirror
        // and still respect any GeckoLib animation playing on them.
        currentPlayer = player
        try {
            val instanceId = player.id.toLong()
            val limbSwing = player.walkAnimation.position(partialTick)
            val limbSwingAmount = player.walkAnimation.speed(partialTick)
            val velSqr = player.deltaMovement.horizontalDistanceSqr()
            val isMoving = velSqr > 0.001
            val animationState = AnimationState<YSMPlayerAnimatable>(
                animatable, limbSwing, limbSwingAmount, partialTick, isMoving
            )
            geoModel.handleAnimations(animatable, instanceId, animationState)

            // Stage 2: bone mirror — vanilla pose flows into bedrock bones.
            HumanoidBoneMirror.apply(vanillaModel, baked)
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
