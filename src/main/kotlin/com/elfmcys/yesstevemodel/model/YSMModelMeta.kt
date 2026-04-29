package com.elfmcys.yesstevemodel.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Top-level shape of `ysm.json` (spec 2, the format used by ysm 2.6.x builtin models).
 *
 * `files` is a bag of entity-typed entries (`player`, `arrow`, `boat`, `projectiles`, ...).
 * Phase 1 only resolves `player`; other entries are kept as raw JSON so later phases can read them.
 */
@Serializable
data class YSMModelMeta(
    val spec: Int = 1,
    val metadata: ModelMetadata,
    val properties: ModelProperties = ModelProperties(),
    val files: ModelFiles,
)

@Serializable
data class ModelMetadata(
    val name: String,
    val tips: String? = null,
    val license: License = License(),
    val authors: List<Author> = emptyList(),
    val link: Map<String, String> = emptyMap(),
)

@Serializable
data class License(
    val type: String = "Unknown",
    val desc: String? = null,
)

@Serializable
data class Author(
    val name: String,
    val role: String? = null,
    val avatar: String? = null,
    val contact: Map<String, String> = emptyMap(),
    val comment: String? = null,
)

/**
 * Player-render-affecting properties plus an opaque GUI-config bag (Phase 5 reads `extra*`).
 */
@Serializable
data class ModelProperties(
    @SerialName("height_scale") val heightScale: Float = 1.0f,
    @SerialName("width_scale") val widthScale: Float = 1.0f,
    val free: Boolean = false,
    @SerialName("default_texture") val defaultTexture: String? = null,
    @SerialName("preview_animation") val previewAnimation: String? = null,
    @SerialName("render_layers_first") val renderLayersFirst: Boolean = false,
    @SerialName("disable_preview_rotation") val disablePreviewRotation: Boolean = false,
    @SerialName("all_cutout") val allCutout: Boolean = false,
    @SerialName("extra_animation") val extraAnimation: JsonElement? = null,
    @SerialName("extra_animation_classify") val extraAnimationClassify: JsonElement? = null,
    @SerialName("extra_animation_buttons") val extraAnimationButtons: JsonElement? = null,
)

@Serializable
data class ModelFiles(
    val player: PlayerFiles,
    @SerialName("animation_controllers") val animationControllers: JsonElement? = null,
)

@Serializable
data class PlayerFiles(
    val model: ModelPaths,
    val animation: Map<String, String> = emptyMap(),
    val texture: List<TextureRef> = emptyList(),
    @SerialName("animation_controllers") val animationControllers: JsonElement? = null,
)

@Serializable
data class ModelPaths(
    val main: String,
    val arm: String? = null,
)
