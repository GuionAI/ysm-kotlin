package com.elfmcys.yesstevemodel.model

import com.elfmcys.yesstevemodel.YSMMod
import net.minecraft.resources.ResourceLocation

/**
 * Resolves a [RegisteredModel] to the resource locations GeckoLib will load from.
 *
 * GeckoLib 4's GeckoLibCache only scans `assets/<namespace>/geo/...` for models and
 * `assets/<namespace>/animations/...` for animations (filtered by .json suffix). The
 * original YSM model authors put files under `builtin/<id>/models/` and
 * `builtin/<id>/animations/`, so our extraction tooling (Phase 4) relocates them:
 *
 *   assets/yesstevemodel/geo/builtin/<id>/main.json
 *   assets/yesstevemodel/geo/builtin/<id>/arm.json
 *   assets/yesstevemodel/animations/builtin/<id>/<key>.animation.json
 *
 * The `ysm.json` manifest stays at `builtin/<id>/ysm.json` since it's loaded by our own
 * code, not GeckoLib. Avatars, lang, and textures stay at their authored paths under
 * `builtin/<id>/` because Minecraft's texture loader doesn't care about prefix.
 */
object YSMResources {
    fun modelRoot(id: String): ResourceLocation =
        ResourceLocation(YSMMod.MOD_ID, "builtin/$id")

    /** Path for the Bedrock geometry file under `geo/`. GeckoLib loads from this path. */
    fun mainModel(model: RegisteredModel): ResourceLocation =
        ResourceLocation(YSMMod.MOD_ID, "geo/builtin/${model.id}/main.json")

    /** Path for the first-person arm geometry. May not exist for every model. */
    fun armModel(model: RegisteredModel): ResourceLocation? =
        if (model.meta.files.player.model.arm != null)
            ResourceLocation(YSMMod.MOD_ID, "geo/builtin/${model.id}/arm.json")
        else null

    // Path for one animation-hook file (main, tac, slashblade, etc.).
    // Filename keeps the original YSM <key>.animation.json convention; GeckoLib's scanner
    // filters by .json suffix only, so the .animation. infix is fine.
    fun animation(model: RegisteredModel, key: String): ResourceLocation? {
        model.meta.files.player.animation[key] ?: return null
        return ResourceLocation(YSMMod.MOD_ID, "animations/builtin/${model.id}/$key.animation.json")
    }

    /** Texture path resolved against the manifest's per-model relative paths. */
    fun texture(model: RegisteredModel, index: Int): ResourceLocation? =
        model.meta.files.player.texture.getOrNull(index)?.let { sibling(model, it.path) }

    fun textures(model: RegisteredModel): List<ResourceLocation> =
        model.meta.files.player.texture.map { sibling(model, it.path) }

    fun sibling(model: RegisteredModel, relative: String): ResourceLocation {
        val metaPath = model.metaLocation.path
        val dir = metaPath.substringBeforeLast('/', missingDelimiterValue = "")
        val joined = if (dir.isEmpty()) relative else "$dir/$relative"
        return ResourceLocation(model.metaLocation.namespace, joined)
    }
}
