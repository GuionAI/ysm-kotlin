package com.elfmcys.yesstevemodel.model

import com.elfmcys.yesstevemodel.YSMMod
import net.minecraft.resources.ResourceLocation

/**
 * Resolves relative paths inside a model's `ysm.json` to fully-qualified [ResourceLocation]s.
 *
 * Example: a model at `assets/yesstevemodel/builtin/wine_fox/01_taisho_maid/ysm.json` with
 * `files.player.model.main = "models/main.json"` resolves to
 * `yesstevemodel:builtin/wine_fox/01_taisho_maid/models/main.json`.
 */
object YSMResources {
    fun modelRoot(id: String): ResourceLocation =
        ResourceLocation(YSMMod.MOD_ID, "builtin/$id")

    fun sibling(model: RegisteredModel, relative: String): ResourceLocation {
        val metaPath = model.metaLocation.path
        val dir = metaPath.substringBeforeLast('/', missingDelimiterValue = "")
        val joined = if (dir.isEmpty()) relative else "$dir/$relative"
        return ResourceLocation(model.metaLocation.namespace, joined)
    }

    fun mainModel(model: RegisteredModel): ResourceLocation =
        sibling(model, model.meta.files.player.model.main)

    fun armModel(model: RegisteredModel): ResourceLocation? =
        model.meta.files.player.model.arm?.let { sibling(model, it) }

    fun animation(model: RegisteredModel, key: String): ResourceLocation? =
        model.meta.files.player.animation[key]?.let { sibling(model, it) }

    fun texture(model: RegisteredModel, index: Int): ResourceLocation? =
        model.meta.files.player.texture.getOrNull(index)?.let { sibling(model, it.path) }

    fun textures(model: RegisteredModel): List<ResourceLocation> =
        model.meta.files.player.texture.map { sibling(model, it.path) }
}
