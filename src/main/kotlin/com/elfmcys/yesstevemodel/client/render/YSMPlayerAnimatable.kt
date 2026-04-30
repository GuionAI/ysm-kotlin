package com.elfmcys.yesstevemodel.client.render

import software.bernie.geckolib.animatable.SingletonGeoAnimatable
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.core.animation.AnimatableManager
import software.bernie.geckolib.util.GeckoLibUtil
import software.bernie.geckolib.util.RenderUtils

/**
 * GeckoLib needs a `GeoAnimatable` to attach animation state to. Player isn't one and we
 * can't make it one without invasive bytecode surgery, so we wrap: a single shared
 * instance handed to GeckoLib whenever a player draws.
 *
 * **Phase 6 architecture: bone mirror as primary animation source.** Movement animations
 * (idle/walk/run/sneak/swim/jump/attack) are NOT driven by GeckoLib here. Instead, vanilla
 * `LivingEntityRenderer.render` calls `HumanoidModel.setupAnim` first (which is where
 * playerAnimator/TACZ/SlashBlade mixins fire and mutate `ModelPart` rotations), and our
 * `HumanoidBoneMirror` copies the final post-mutation `HumanoidModel` state onto bedrock
 * bones. This gives us automatic compatibility with every mod that animates the player by
 * mutating `HumanoidModel` — no per-mod integration needed.
 *
 * No animation controllers are registered. YSM-specific extras (dance, wave, GUI poses)
 * will get a separate controller in Phase 5/7 when the GUI selector lands and triggers
 * them by keybind. Those play *over* the bone mirror for bones the bone mirror doesn't
 * touch (decorative bones — cape, hat, ribbons, ears).
 */
object YSMPlayerAnimatable : SingletonGeoAnimatable {
    private val cache: AnimatableInstanceCache = GeckoLibUtil.createInstanceCache(this)

    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) {
        // Intentionally empty. Phase 5/7 may add controllers for YSM-specific extras
        // (dance/wave/etc.) that don't have a vanilla equivalent. Movement animations
        // come from the bone mirror via vanilla setupAnim, not from here.
    }

    override fun getAnimatableInstanceCache(): AnimatableInstanceCache = cache

    override fun getTick(obj: Any?): Double = RenderUtils.getCurrentTick()
}
