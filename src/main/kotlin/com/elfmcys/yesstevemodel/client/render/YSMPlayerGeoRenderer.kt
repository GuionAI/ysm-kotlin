package com.elfmcys.yesstevemodel.client.render

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import software.bernie.geckolib.cache.`object`.BakedGeoModel
import software.bernie.geckolib.model.GeoModel
import software.bernie.geckolib.renderer.GeoRenderer
import software.bernie.geckolib.renderer.layer.GeoRenderLayer

/**
 * Minimal [GeoRenderer] implementation. Sidesteps `GeoEntityRenderer` /
 * `GeoReplacedEntityRenderer` (both extend `EntityRenderer`, want a registration context we
 * don't have inside a mixin redirect handler).
 *
 * Layer-event firing methods are no-ops: we don't ship custom render layers in Phase 2; armor
 * and item-in-hand are still rendered by the vanilla layers loop after our redirect returns.
 */
object YSMPlayerGeoRenderer : GeoRenderer<YSMPlayerAnimatable> {

    private val geoModel: YSMPlayerGeoModel = YSMPlayerGeoModel()
    private val emptyLayers: List<GeoRenderLayer<YSMPlayerAnimatable>> = emptyList()

    override fun getGeoModel(): GeoModel<YSMPlayerAnimatable> = geoModel

    override fun getAnimatable(): YSMPlayerAnimatable = YSMPlayerAnimatable

    override fun getRenderLayers(): List<GeoRenderLayer<YSMPlayerAnimatable>> = emptyLayers

    override fun fireCompileRenderLayersEvent() = Unit
    override fun firePreRenderEvent(poseStack: PoseStack, model: BakedGeoModel, bufferSource: MultiBufferSource, partialTick: Float, packedLight: Int): Boolean = true
    override fun firePostRenderEvent(poseStack: PoseStack, model: BakedGeoModel, bufferSource: MultiBufferSource, partialTick: Float, packedLight: Int) = Unit
    override fun updateAnimatedTextureFrame(animatable: YSMPlayerAnimatable) = Unit
}
