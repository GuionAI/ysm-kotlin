package com.elfmcys.yesstevemodel.mixin;

import com.elfmcys.yesstevemodel.client.render.YSMRenderBridge;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Replaces only the body-mesh draw inside {@code LivingEntityRenderer.render} when the entity
 * is a player with an active YSM model. Vanilla {@code setupAnim} runs first (so playerAnimator,
 * TACZ, SlashBlade all get their pose mutations in), then we read the resulting
 * {@code HumanoidModel} state and render the GeckoLib bedrock model in its place. The vanilla
 * layers loop continues normally after this returns.
 *
 * <p>See {@code decisions/04-player-renderer-strategy.md} for the why; R1 (priority conflict
 * with playerAnimator) is verified non-issue — playerAnimator's redirects target different
 * call sites (Iterator.next inside layers loop, super.render inside PlayerRenderer.render).
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {

    @Redirect(
        method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"
        )
    )
    private void ysm$replaceBodyDraw(
        // Redirected method args (receiver type matches the call site's declared class):
        EntityModel<?> model, PoseStack poseStack, VertexConsumer buffer,
        int packedLight, int packedOverlay, float r, float g, float b, float a,
        // Outer-method args, captured by trailing position:
        LivingEntity entity, float entityYaw, float partialTick,
        PoseStack outerPoseStack, MultiBufferSource bufferSource, int outerPackedLight
    ) {
        if (entity instanceof AbstractClientPlayer player
            && model instanceof HumanoidModel<?> humanoidModel
            && YSMRenderBridge.INSTANCE.shouldReplace(player)) {
            @SuppressWarnings("unchecked")
            HumanoidModel<? extends LivingEntity> typed = (HumanoidModel<? extends LivingEntity>) humanoidModel;
            YSMRenderBridge.INSTANCE.renderGeo(player, typed, partialTick, poseStack, bufferSource, packedLight);
        } else {
            model.renderToBuffer(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        }
    }
}
