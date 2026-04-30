package com.elfmcys.yesstevemodel.client.render

import com.elfmcys.yesstevemodel.model.YSMResources
import net.minecraft.client.player.AbstractClientPlayer
import software.bernie.geckolib.animatable.SingletonGeoAnimatable
import software.bernie.geckolib.cache.GeckoLibCache
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.core.animation.AnimatableManager
import software.bernie.geckolib.core.animation.AnimationController
import software.bernie.geckolib.core.animation.RawAnimation
import software.bernie.geckolib.core.`object`.PlayState
import software.bernie.geckolib.util.GeckoLibUtil
import software.bernie.geckolib.util.RenderUtils

/**
 * GeckoLib's animation engine wants a `GeoAnimatable` instance to attach state to. Player
 * isn't one, and we can't make it implement the interface without invasive surgery, so we
 * keep a single wrapper instance that the renderer hands to GeckoLib whenever a player
 * draws.
 *
 * The current rendering player is stashed on [YSMRenderBridge.currentPlayer] before
 * `handleAnimations` runs so the controller's state handler can read player state without
 * the AnimationState surface having to carry it.
 *
 * Animations are looked up by key in the active model's `main.animation.json`. Standard
 * keys observed across the 19 ysm-2.6.x builtins: `idle`, `walk`, `run`, `sneak`,
 * `swim`, `swim_stand`, `jump`, `fly`, `elytra_fly`, `attacked`, `sit`, `sleep`. Each
 * model authors slightly different content under each key but the key set is consistent.
 */
object YSMPlayerAnimatable : SingletonGeoAnimatable {
    private val cache: AnimatableInstanceCache = GeckoLibUtil.createInstanceCache(this)

    /**
     * For each abstract state, the candidate animation key names to try in order. The first
     * one present in the active model's main.animation.json is used. The fallback chain
     * covers naming variation across YSM models — e.g. some models ship `swim` (horizontal
     * swim pose), some ship `swim_stand` (treading-water vertical pose), and a few have
     * both. We prefer the more specific names first and degrade toward IDLE.
     */
    private val IDLE_KEYS = arrayOf("idle")
    private val WALK_KEYS = arrayOf("walk")
    private val RUN_KEYS = arrayOf("run")
    private val SNEAK_KEYS = arrayOf("sneaking", "sneak")
    private val JUMP_KEYS = arrayOf("jump")
    private val SWIM_KEYS = arrayOf("swim", "swim_stand")
    private val SWIM_STAND_KEYS = arrayOf("swim_stand", "swim")
    private val FLY_KEYS = arrayOf("elytra_fly", "fly")

    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) {
        controllers.add(
            AnimationController<YSMPlayerAnimatable>(
                this, "main", /*transitionLengthTicks*/ 5
            ) { state ->
                state.setAndContinue(pickAnimation(state.isMoving))
                PlayState.CONTINUE
            }
        )
    }

    /**
     * Returns the [RawAnimation] that best matches the current rendering player's state.
     *
     * Falls back to "idle" if no player is set (e.g. preview rendering, or a frame where
     * the bridge hasn't populated `currentPlayer` yet). All decisions read live state from
     * the same [AbstractClientPlayer] the renderer is drawing — no copy, so transitions
     * happen on the same tick the player input fires.
     *
     * Per-state animation keys are looked up via [firstAvailable] which queries the model's
     * actual loaded animation set; this lets a model with only `swim_stand` (no `swim`)
     * still play a swim animation when the player is swimming.
     */
    private fun pickAnimation(isMoving: Boolean): RawAnimation {
        val player: AbstractClientPlayer = YSMRenderBridge.currentPlayer ?: return loop(IDLE_KEYS)
        return when {
            player.isFallFlying -> loop(FLY_KEYS)
            player.isSwimming -> loop(SWIM_KEYS)             // active horizontal swim
            player.isInWater && !player.onGround() -> loop(SWIM_STAND_KEYS)  // treading water
            player.isShiftKeyDown -> loop(SNEAK_KEYS)
            !isMoving -> loop(IDLE_KEYS)
            !player.onGround() -> loop(JUMP_KEYS)
            player.isSprinting -> loop(RUN_KEYS)
            else -> loop(WALK_KEYS)
        }
    }

    /** Builds a `RawAnimation` for the first key in [candidates] that the active model declares. */
    private fun loop(candidates: Array<String>): RawAnimation =
        RawAnimation.begin().thenLoop(firstAvailable(candidates) ?: candidates[0])

    /**
     * Returns the first animation name in [candidates] that exists in the active model's
     * main.animation.json. Returns null only if none of them exist (the model hasn't loaded
     * yet, or the animation file failed to parse). Caller should pass `candidates[0]` as
     * fallback in that case.
     */
    private fun firstAvailable(candidates: Array<String>): String? {
        val model = YSMRenderBridge.activeModel ?: return null
        val animationFile = YSMResources.animation(model, "main") ?: return null
        val baked = GeckoLibCache.getBakedAnimations()[animationFile] ?: return null
        for (name in candidates) {
            if (baked.animations()[name] != null) return name
        }
        return null
    }

    override fun getAnimatableInstanceCache(): AnimatableInstanceCache = cache

    override fun getTick(obj: Any?): Double = RenderUtils.getCurrentTick()
}
