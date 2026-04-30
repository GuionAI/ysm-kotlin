package com.elfmcys.yesstevemodel.client.render

import net.minecraft.client.model.HumanoidModel
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.world.entity.LivingEntity
import software.bernie.geckolib.cache.`object`.BakedGeoModel

/**
 * Copies the post-`setupAnim` pose state of a vanilla [HumanoidModel] onto the matching
 * bones of a baked GeckoLib model.
 *
 * "Post-setupAnim" is the key — by the time the mixin redirect runs, vanilla's setupAnim
 * has already finished AND every mod that mutates `HumanoidModel` (playerAnimator, TACZ,
 * SlashBlade, emotes) has layered its work on top. We just project the final ModelPart
 * state onto Bedrock bones, getting compat with all those mods for free.
 *
 * **Bone naming convention** (verified across the ysm-2.6.x test fixtures): PascalCase.
 * `Head`, `RightArm`, `LeftArm`, `RightLeg`, `LeftLeg`. `getBone(name)` does a recursive
 * search so depth in the bedrock hierarchy doesn't matter.
 *
 * **Axis-sign convention** (R6 fix attempt): the bedrock model is rendered under an X
 * mirror (vanilla's `scale(-1,-1,1)` plus our `scale(1,-1,1)` compose to a net X flip).
 * Reflections reverse rotation direction around the axes perpendicular to the flip axis:
 *   - X rotation: invariant under X-flip → direct copy
 *   - Y rotation: sign reversed under X-flip → negate
 *   - Z rotation: sign reversed under X-flip → negate
 * If empirically the swing direction is still wrong we'll iterate on these signs.
 *
 * **Body rotation is intentionally NOT mirrored.** Vanilla's `body`, `head`, arms, legs
 * are sibling top-level ModelParts; rotating `body.xRot` alone tilts only the torso.
 * Bedrock `Head` and arms are nested under `UpperBody`, so setting `UpperBody.rot =
 * body.rot` would double-rotate everything below. Proper handling needs quaternion
 * composition (Head.localRot = inverse(parent body rot) * vanilla.head.rot). Phase 7
 * polish; for now sneak/swim body bend stays absent on the bedrock model.
 */
object HumanoidBoneMirror {
    fun apply(humanoidModel: HumanoidModel<out LivingEntity>, bakedGeoModel: BakedGeoModel) {
        copyPart(humanoidModel.head, bakedGeoModel, "Head")
        copyPart(humanoidModel.rightArm, bakedGeoModel, "RightArm")
        copyPart(humanoidModel.leftArm, bakedGeoModel, "LeftArm")
        copyPart(humanoidModel.rightLeg, bakedGeoModel, "RightLeg")
        copyPart(humanoidModel.leftLeg, bakedGeoModel, "LeftLeg")
    }

    private fun copyPart(part: ModelPart, bakedGeoModel: BakedGeoModel, boneName: String) {
        val bone = bakedGeoModel.getBone(boneName).orElse(null) ?: return
        // R6: X is the flip axis (invariant under reflection); Y and Z rotations have
        // their sign reversed when projected through an X-flip. Empirically iterate if
        // wrong arm or wrong direction reappears.
        bone.setRotX(part.xRot)
        bone.setRotY(-part.yRot)
        bone.setRotZ(-part.zRot)
    }
}
