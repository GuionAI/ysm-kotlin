package com.elfmcys.yesstevemodel.client.render

import com.elfmcys.yesstevemodel.YSMMod
import com.elfmcys.yesstevemodel.model.YSMResources
import net.minecraft.resources.ResourceLocation
import software.bernie.geckolib.model.GeoModel

/**
 * Tells GeckoLib where to find the active YSM model's geo / animation / texture for the player.
 *
 * The active model is read from [YSMRenderBridge] each frame so model switching at runtime is
 * a single bridge mutation, not a renderer rewire.
 *
 * If no model is active, this falls back to placeholder paths inside our own jar so GeckoLib
 * doesn't NPE on first call. The mixin checks [YSMRenderBridge.shouldReplace] before invoking
 * us, so the fallback path should never actually be loaded in practice.
 */
class YSMPlayerGeoModel : GeoModel<YSMPlayerAnimatable>() {

    private val fallbackModel = ResourceLocation(YSMMod.MOD_ID, "geo/missing.geo.json")
    private val fallbackTexture = ResourceLocation(YSMMod.MOD_ID, "textures/missing.png")
    private val fallbackAnimation = ResourceLocation(YSMMod.MOD_ID, "animations/missing.animation.json")

    override fun getModelResource(animatable: YSMPlayerAnimatable): ResourceLocation =
        YSMRenderBridge.activeModel?.let { YSMResources.mainModel(it) } ?: fallbackModel

    override fun getTextureResource(animatable: YSMPlayerAnimatable): ResourceLocation =
        YSMRenderBridge.activeModel?.let { YSMResources.texture(it, 0) } ?: fallbackTexture

    override fun getAnimationResource(animatable: YSMPlayerAnimatable): ResourceLocation {
        val model = YSMRenderBridge.activeModel ?: return fallbackAnimation
        return YSMResources.animation(model, "main") ?: fallbackAnimation
    }
}
