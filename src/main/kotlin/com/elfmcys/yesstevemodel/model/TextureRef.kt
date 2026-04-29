package com.elfmcys.yesstevemodel.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * One entry in the `files.player.texture` array. The 2.6.x format mixes two shapes:
 * - bare string: `"textures/skin.png"`
 * - object: `{"uv": "textures/texture.png"}` (sometimes with extra keys, e.g. `emissive`)
 *
 * `path` always points at the diffuse/UV texture so the renderer can stay agnostic.
 * Extra object keys are preserved on `extras` so later phases (emissive maps, etc.) can read them.
 */
@Serializable(with = TextureRefSerializer::class)
data class TextureRef(
    val path: String,
    val extras: Map<String, String> = emptyMap(),
)

internal object TextureRefSerializer : KSerializer<TextureRef> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("com.elfmcys.yesstevemodel.model.TextureRef")

    override fun deserialize(decoder: Decoder): TextureRef {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("TextureRef requires a JSON decoder")
        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonPrimitive -> {
                if (!element.isString) throw SerializationException("texture entry must be a string or object, got $element")
                TextureRef(path = element.content)
            }
            is JsonObject -> parseObject(element)
            else -> throw SerializationException("texture entry must be a string or object, got $element")
        }
    }

    private fun parseObject(obj: JsonObject): TextureRef {
        val uv = obj["uv"]?.jsonPrimitive?.contentOrNull()
            ?: throw SerializationException("texture object missing 'uv' key: $obj")
        val extras = obj.entries
            .filter { it.key != "uv" }
            .mapNotNull { (k, v) -> (v as? JsonPrimitive)?.contentOrNull()?.let { k to it } }
            .toMap()
        return TextureRef(path = uv, extras = extras)
    }

    override fun serialize(encoder: Encoder, value: TextureRef) {
        if (value.extras.isEmpty()) {
            encoder.encodeSerializableValue(String.serializer(), value.path)
        } else {
            val obj = buildMap<String, JsonPrimitive> {
                put("uv", JsonPrimitive(value.path))
                value.extras.forEach { (k, v) -> put(k, JsonPrimitive(v)) }
            }
            encoder.encodeSerializableValue(JsonObject.serializer(), JsonObject(obj))
        }
    }
}

private fun JsonPrimitive.contentOrNull(): String? = if (isString) content else null
