package com.elfmcys.yesstevemodel.client.render

import net.minecraft.client.player.AbstractClientPlayer
import software.bernie.geckolib.animatable.SingletonGeoAnimatable
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

    private val IDLE: RawAnimation = RawAnimation.begin().thenLoop("idle")
    private val WALK: RawAnimation = RawAnimation.begin().thenLoop("walk")
    private val RUN: RawAnimation = RawAnimation.begin().thenLoop("run")
    private val SNEAK: RawAnimation = RawAnimation.begin().thenLoop("sneak")
    private val SWIM: RawAnimation = RawAnimation.begin().thenLoop("swim")
    private val FLY: RawAnimation = RawAnimation.begin().thenLoop("elytra_fly")
    private val JUMP: RawAnimation = RawAnimation.begin().thenLoop("jump")

    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) {
        controllers.add(
            AnimationController<YSMPlayerAnimatable>(
                this, "main", /*transitionLengthTicks*/ 5
            ) { state ->
                val animation = pickAnimation(state.isMoving)
                state.setAndContinue(animation)
                PlayState.CONTINUE
            }
        )
    }

    /**
     * Returns the [RawAnimation] that best matches the current rendering player's state.
     *
     * Falls back to [IDLE] if no player is set (e.g. preview rendering, or a frame where
     * the bridge hasn't populated `currentPlayer` yet). All decisions read live state from
     * the same [AbstractClientPlayer] the renderer is drawing — no copy, so transitions
     * happen on the same tick the player input fires.
     */
    private fun pickAnimation(isMoving: Boolean): RawAnimation {
        val player: AbstractClientPlayer = YSMRenderBridge.currentPlayer ?: return IDLE
        return when {
            player.isFallFlying -> FLY
            player.isSwimming -> SWIM
            player.isShiftKeyDown -> SNEAK
            !isMoving -> IDLE
            !player.onGround() -> JUMP
            player.isSprinting -> RUN
            else -> WALK
        }
    }

    override fun getAnimatableInstanceCache(): AnimatableInstanceCache = cache

    override fun getTick(obj: Any?): Double = RenderUtils.getCurrentTick()
}
