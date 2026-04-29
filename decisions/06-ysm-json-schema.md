# 06 — `ysm.json` schema realities (spec 2)

**Status:** Locked for the spec-2 format. Phase 4 may add extraction tooling.

## What's the problem?

Each YSM model is a directory of files: a Bedrock geometry (`.geo.json`), several animation files (`.animation.json`), and one or more textures (`.png`). The directory also contains a `ysm.json` manifest that names the model, lists its files, and carries metadata (author, license, GUI form definitions for skin/expression/pose pickers).

Phase 1 needs to parse `ysm.json` cleanly. The Phase 1 task file in `tasks/` had a simplified shape that didn't match what the actual ysm-2.6.2 jar ships. This doc records the real schema for future reference.

## Source of truth

`/Users/neil/Code/guion-opensource/minecraft/.minecraft/mods/ysm-2.6.2-forge+mc1.20.1-release.jar` at `assets/yes_steve_model/builtin/<id>/ysm.json`. 19 builtins:

- `default` (special — has the full animation set, used as a fallback for other models)
- `misc/1_alex`, `misc/2_steve`, `misc/3_default_boy`, `misc/4_default_controllers`
- `wine_fox/01_taisho_maid` through `wine_fox/15_kluonoa` (14 models)

Four representative samples are committed at `src/test/resources/ysm-samples/` (default, steve, new_year, sta) covering the schema variants.

## Top-level shape

```jsonc
{
  "spec": 2,                    // version of this manifest format
  "metadata": { ... },          // human-readable info
  "properties": { ... },        // engine-readable config + GUI form definitions
  "files": {
    "player": { ... },          // the player-render assets — Phase 1 reads this
    "arrow": { ... },           // entity extras — out of scope for Phase 1
    "projectiles": { ... },     // ditto
    "vehicles": { ... },        // ditto
    "sound_path": "..."         // optional
  }
}
```

## The four schema gotchas worth knowing

### G1: `texture` entries are polymorphic

`files.player.texture` is an array, but each entry is *either*:
```json
"textures/skin.png"
```
or
```json
{ "uv": "textures/x.png", "emissive": "textures/x_emissive.png" }
```

Most builtins use the bare-string form. `wine_fox/08_sta` uses the object form for one variant. Our `TextureRef` data class normalizes both into `{path, extras}` via a custom Kotlin serializer.

### G2: `animation` keys are open-ended mod hooks

`files.player.animation` is `Map<String, String>` — keys are mod-specific animation hook names, *not* a closed enum. Examples seen in the wild:

```
main, arm, extra, tac, carryon, swem, parcool, slashblade,
tlm, immersive_melodies, irons_spell_books, fp_arm
```

The `default` builtin ships every hook. Per-model jsons declare only the hooks they animate.

**Implication for Phase 6 (mod compat):** the `tac`, `slashblade`, `parcool` etc. keys are exactly the integration points. When TACZ aims, our renderer triggers the GeckoLib animation registered under the `tac` key for the active model. The animation key naming is the API contract between YSM authors and mod-compat authors.

### G3: GUI form definitions live in `properties.extra_animation*`

`properties.extra_animation`, `properties.extra_animation_classify`, and `properties.extra_animation_buttons` are huge nested structures. They drive the YSM in-game model picker — sliders and radios for things like "eye size: -100 to 50", "outfit variant: a/b/c", etc. Each control sets a molang variable read by the bedrock model's animations (e.g. `v.player_eyeballs`).

Phase 1 keeps these as `kotlinx.serialization.json.JsonElement` (opaque). Phase 5 (GUI) parses them into a typed UI tree.

`wine_fox/08_sta` is the largest example — its `ysm.json` is 19 KB, mostly `extra_animation_buttons`.

### G4: `metadata.license` flavors

Two license types observed:
- `"CC 0"` — `properties.free` is `true`, no auth needed
- `"CC BY-NC-SA 4.0"` — `properties.free` is `false` for these but the actual gating mechanism in YSM was server-side authentication via the embedded encryption blob, which we're not reimplementing.

For our open-source replacement, `properties.free` is informational only — there's no DRM. Authors who licensed their models under non-free terms when distributing through original YSM are *not* implicitly licensing them under those terms here. License compliance for built-in models is a [phase 4 / extraction-time] concern, not a runtime one.

## What does this mean later?

- **Phase 4 (built-in extraction)** must extract per-model `models/`, `animations/`, `textures/`, `avatar/`, and `lang/` directories — not just `ysm.json`. Avatar paths are referenced from `metadata.authors[].avatar`, surfaced by the GUI in Phase 5.
- **Phase 5 (GUI)** parses `properties.extra_animation*`. The form types observed: `range` (slider), `checkbox`, `radio`. Values are molang strings.
- **Phase 6 (mod compat)** uses the `animation` map keys as integration points. New mod compat = new key + an `AnimationController` registration.
- **Don't constrain `animation` keys at the type level.** Open `Map<String, String>` is the right call. Adding a hook for, say, `epicfight` requires zero schema change.
