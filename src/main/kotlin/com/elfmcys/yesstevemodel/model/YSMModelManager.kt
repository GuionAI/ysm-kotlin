package com.elfmcys.yesstevemodel.model

import com.elfmcys.yesstevemodel.YSMMod
import kotlinx.serialization.json.Json
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager

/**
 * Holds the active set of YSM models discovered from the resource manager.
 *
 * A YSM model is identified by an opaque string id derived from its directory layout under
 * `assets/yesstevemodel/builtin/<id>/ysm.json`. Slashes in the id are preserved so we can
 * round-trip ids like `wine_fox/01_taisho_maid`.
 */
object YSMModelManager {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private const val BUILTIN_PREFIX = "builtin/"
    private const val META_FILENAME = "ysm.json"

    @Volatile
    private var models: Map<String, RegisteredModel> = emptyMap()

    val all: Collection<RegisteredModel> get() = models.values

    fun get(id: String): RegisteredModel? = models[id]

    fun ids(): Set<String> = models.keys

    /**
     * Re-scans the resource manager for every `assets/yesstevemodel/builtin/<id>/ysm.json`
     * and rebuilds the registry. Safe to call from a resource-reload listener.
     */
    fun reload(resourceManager: ResourceManager) {
        val discovered = LinkedHashMap<String, RegisteredModel>()
        val resources = resourceManager.listResources(YSMMod.MOD_ID) { path ->
            path.path.startsWith(BUILTIN_PREFIX) && path.path.endsWith("/$META_FILENAME")
        }
        for ((location, resource) in resources) {
            val id = idFromMetaLocation(location) ?: continue
            try {
                val meta = resource.open().use { stream ->
                    stream.bufferedReader(Charsets.UTF_8).use { reader ->
                        json.decodeFromString(YSMModelMeta.serializer(), reader.readText())
                    }
                }
                discovered[id] = RegisteredModel(id, location, meta)
            } catch (e: Exception) {
                YSMMod.LOGGER.error("Failed to load YSM model '{}' from {}: {}", id, location, e.message)
            }
        }
        models = discovered
        YSMMod.LOGGER.info("YSM models loaded: {}", discovered.keys)
    }

    /**
     * Parse a single ysm.json from raw text. Used by tests and out-of-game tooling.
     */
    fun parse(jsonText: String): YSMModelMeta =
        json.decodeFromString(YSMModelMeta.serializer(), jsonText)

    private fun idFromMetaLocation(location: ResourceLocation): String? {
        val path = location.path
        if (!path.startsWith(BUILTIN_PREFIX)) return null
        if (!path.endsWith("/$META_FILENAME")) return null
        return path.substring(BUILTIN_PREFIX.length, path.length - META_FILENAME.length - 1)
            .takeIf { it.isNotEmpty() }
    }
}

/**
 * A model that the manager has successfully parsed.
 *
 * `metaLocation` points at the ysm.json itself; sibling files (models, animations, textures)
 * are addressed via [YSMResources.sibling] using the relative paths inside [meta].
 */
data class RegisteredModel(
    val id: String,
    val metaLocation: ResourceLocation,
    val meta: YSMModelMeta,
)
