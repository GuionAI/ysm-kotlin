package com.elfmcys.yesstevemodel.mixin;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Replaces the body-mesh draw call inside {@code LivingEntityRenderer.render} when the entity
 * is a player with an active YSM model. See {@code decisions/04-player-renderer-strategy.md}.
 *
 * <p>The actual {@code @Redirect} on {@code Model.renderToBuffer} is added once the bridge
 * (YSMRenderBridge) and bone mirror exist. This stub establishes the mixin wiring so we can
 * verify the gradle/refmap pipeline before writing render code.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
}
