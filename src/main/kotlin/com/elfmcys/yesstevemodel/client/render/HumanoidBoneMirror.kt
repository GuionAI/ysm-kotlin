package com.elfmcys.yesstevemodel.client.render

import net.minecraft.client.model.HumanoidModel
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.world.entity.LivingEntity
import software.bernie.geckolib.cache.`object`.BakedGeoModel

/**
 * Copies the post-`setupAnim` pose state of a vanilla [HumanoidModel] onto the matching bones
 * of a baked GeckoLib model.
 *
 * "Post-setupAnim" is the key — by the time the mixin redirect runs, vanilla's setupAnim has
 * already finished AND playerAnimator/TACZ/SlashBlade mixins have all layered their own
 * mutations on top. We just read the final ModelPart state and project it onto Bedrock bones.
 *
 * **Bone-name convention discovered from ysm-2.6.x test fixture (`misc/2_steve`):**
 * - `Head`, `RightArm`, `LeftArm`, `RightLeg`, `LeftLeg` (PascalCase)
 * - The bedrock hierarchy is much deeper than vanilla — `Head` nests under
 *   `UpperBody → UpBody → AllBody → MAllBody → Root`. `RightArm`/`LeftArm` nest under
 *   `Arm → UpperBody`. Legs nest under `DownBody → AllBody → MAllBody → Root`.
 * - `getBone(...)` does a recursive search so depth doesn't matter for lookup.
 *
 * **Body rotation is intentionally NOT mirrored** in Phase 2. In vanilla, `head`/`body`/arms/
 * legs are sibling top-level parts; rotating `body.rot` only tilts the body mesh and leaves
 * head/arms in place. In bedrock, `Head` and arms are *children* of `UpperBody`, so setting
 * `UpperBody.rot = body.rot` would double-rotate everything below it. Phase 3 will compose
 * the rotations correctly (likely Head.rot = body.rot⁻¹ × vanilla.head.rot). For now,
 * sneak/swim/etc. body bends will not show on the bedrock model.
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
        bone.setRotX(part.xRot)
        bone.setRotY(part.yRot)
        bone.setRotZ(part.zRot)
    }
}
