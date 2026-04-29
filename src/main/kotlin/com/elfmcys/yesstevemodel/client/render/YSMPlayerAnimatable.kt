package com.elfmcys.yesstevemodel.client.render

import software.bernie.geckolib.animatable.SingletonGeoAnimatable
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.core.animation.AnimatableManager
import software.bernie.geckolib.util.GeckoLibUtil
import software.bernie.geckolib.util.RenderUtils

/**
 * GeckoLib's animation engine wants a `GeoAnimatable` instance to attach state to. Player
 * isn't one, and we can't make it implement the interface without invasive surgery, so we keep
 * a single wrapper instance that the renderer hands to GeckoLib whenever a player draws.
 *
 * Phase 2 minimum: no animation controllers are registered. The bone mirror (driven by the
 * vanilla HumanoidModel poses) drives bedrock bones each frame, and GeckoLib only acts as the
 * Bedrock-format mesh drawing engine. Phase 3 will add controllers for the YSM `extra`
 * animation set (dance/pose/etc.) that overlay additively on top of the mirrored baseline.
 */
object YSMPlayerAnimatable : SingletonGeoAnimatable {
    private val cache: AnimatableInstanceCache = GeckoLibUtil.createInstanceCache(this)

    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) {
        // Phase 3 will populate this.
    }

    override fun getAnimatableInstanceCache(): AnimatableInstanceCache = cache

    override fun getTick(obj: Any?): Double = RenderUtils.getCurrentTick()
}
