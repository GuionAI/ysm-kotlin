package com.elfmcys.yesstevemodel.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class YSMModelMetaTest {
    private fun load(name: String): String =
        requireNotNull(javaClass.classLoader.getResourceAsStream("ysm-samples/$name")) {
            "fixture ysm-samples/$name not on classpath"
        }.bufferedReader(Charsets.UTF_8).use { it.readText() }

    @Test
    fun `parses minimal steve metadata`() {
        val meta = YSMModelManager.parse(load("steve.json"))
        assertEquals(2, meta.spec)
        assertEquals("Steve （史蒂夫）", meta.metadata.name)
        assertEquals("CC 0", meta.metadata.license.type)
        assertTrue(meta.properties.free)
        assertEquals("models/main.json", meta.files.player.model.main)
        assertEquals("models/arm.json", meta.files.player.model.arm)
        assertEquals(1, meta.files.player.texture.size)
        assertEquals("textures/tartaric_acid.png", meta.files.player.texture[0].path)
    }

    @Test
    fun `parses wine_fox new_year with bare-string textures`() {
        val meta = YSMModelManager.parse(load("new_year.json"))
        assertEquals("New Year Wine Fox（新春酒狐）", meta.metadata.name)
        assertEquals(2, meta.metadata.authors.size)
        assertEquals("textures/skin.png", meta.files.player.texture[0].path)
        assertTrue(meta.files.player.texture[0].extras.isEmpty())
        // Animation map keys are arbitrary mod-specific names, not a fixed enum
        assertTrue(meta.files.player.animation.keys.containsAll(setOf("main", "extra", "tac", "carryon")))
    }

    @Test
    fun `parses wine_fox sta with object-shaped textures and gui properties`() {
        val meta = YSMModelManager.parse(load("sta.json"))
        // texture is `[{"uv": "..."}]` — the object form
        assertEquals("textures/texture.png", meta.files.player.texture[0].path)
        // GUI form definitions are kept as opaque JsonElement for Phase 5 to parse
        assertNotNull(meta.properties.extraAnimationButtons)
        assertNotNull(meta.properties.extraAnimationClassify)
        // Non-free model
        assertEquals(false, meta.properties.free)
        // Scaled body
        assertEquals(0.7f, meta.properties.heightScale)
        assertEquals(0.7f, meta.properties.widthScale)
    }

    @Test
    fun `parses default with full animation set`() {
        val meta = YSMModelManager.parse(load("default.json"))
        val anims = meta.files.player.animation.keys
        // The 'default' builtin ships every supported animation hook
        listOf("main", "arm", "extra", "tac", "carryon", "parcool", "swem", "slashblade", "tlm")
            .forEach { hook -> assertTrue(hook in anims, "expected animation hook '$hook' in default model") }
    }

    @Test
    fun `unknown keys are ignored`() {
        // Inject a top-level field the schema doesn't know about
        val raw = load("steve.json").replace("\"spec\": 2,", "\"spec\": 2, \"unknown_field\": [1,2,3],")
        val meta = YSMModelManager.parse(raw)
        assertEquals("Steve （史蒂夫）", meta.metadata.name)
    }

    @Test
    fun `texture serializer round-trips bare string`() {
        val ref = TextureRef("textures/skin.png")
        val text = kotlinx.serialization.json.Json.encodeToString(TextureRef.serializer(), ref)
        assertEquals("\"textures/skin.png\"", text)
        val back = kotlinx.serialization.json.Json.decodeFromString(TextureRef.serializer(), text)
        assertEquals(ref, back)
    }

    @Test
    fun `texture serializer round-trips object form with extras`() {
        val ref = TextureRef("textures/x.png", mapOf("emissive" to "textures/x_e.png"))
        val text = kotlinx.serialization.json.Json.encodeToString(TextureRef.serializer(), ref)
        // object form: must contain both keys
        assertTrue(text.contains("\"uv\":\"textures/x.png\""))
        assertTrue(text.contains("\"emissive\":\"textures/x_e.png\""))
        val back = kotlinx.serialization.json.Json.decodeFromString(TextureRef.serializer(), text)
        assertEquals(ref, back)
    }

    @Test
    fun `manager id derivation`() {
        // Indirect: parse() works without a ResourceManager; id derivation lives behind reload().
        // Sanity-check that parse handles all fixtures without throwing.
        listOf("steve.json", "new_year.json", "sta.json", "default.json").forEach {
            assertNotNull(YSMModelManager.parse(load(it)))
        }
    }
}
