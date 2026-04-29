# Phase 4: Built-in Model Resources

## Objective
Extract and organize the 19 built-in models from `ysm-2.6.2-forge+mc1.20.1-release.jar` into our mod's resource directory structure.

## Context
The YSM 2.6.2 mod jar contains 19 unencrypted built-in models in standard Bedrock format. These are located at `assets/yes_steve_model/builtin/` inside the jar. We need to extract them and organize them for our mod's resource loading.

**Important**: These are `folder format` models (not encrypted .ysm files). The geo.json, animation.json, and PNG files are in standard Bedrock format and can be read directly.

## Model Inventory

### Default Series (4 models, CC 0 free license)
- `default/` — Default model (5 skins + 9 prop textures, extra animations: dance, wave, clap, etc.)
- `default_alex/` — Alex variant (4 skins)
- `default_steve/` — Steve variant (4 skins)
- `default_boy/` — Default Boy variant (4 skins)

### Wine Fox Series (15 models, mostly CC BY-NC-SA)
- `wine_fox/01_taisho_maid/` — 大正女仆 (14 skins)
- `wine_fox/02_new_year/` — 新春酒狐 (3 skins)
- `wine_fox/03_astronaut/` — 宇航员酒狐 (5 skins)
- `wine_fox/04_kongfu/` — 功夫酒狐 (7 skins)
- `wine_fox/05_magical/` — 魔法酒狐 (7 skins)
- `wine_fox/06_hanfu/` — 汉服酒狐 (1 skin)
- `wine_fox/07_jk/` — JK酒狐 (6 skins)
- `wine_fox/08_sta/` — 斯塔·柏 (4 skins, has SlashBlade compat animation)
- `wine_fox/09_hailuo/` — 星屑海螺 (2 skins, All Rights Reserved)
- `wine_fox/10_zhiban/` — 纸板 (2 skins, All Rights Reserved)
- `wine_fox/11_salesperson/` — 店员酒狐 (3 skins)
- `wine_fox/12_little/` — 小酒狐 (4 skins)
- `wine_fox/13_matured/` — 大酒狐 (1 skin)
- `wine_fox/14_momo/` — MoMo酒狐 (4 skins)
- `wine_fox/15_kluonoa/` — K螺诺亚 (2 skins)

Total: 19 models, 84+ skin textures, 98 animation files, 37.3 MB

## Tasks

### 4.1 Extract models from YSM jar
Write a script to extract from the YSM 2.6.2 jar:
```bash
# Source: ~/Downloads/落幕曲/落幕曲懒人包/落幕曲1.6.3解压包*.zip
# Inside zip: .minecraft/versions/1.6.3/mods/ysm-2.6.2-forge+mc1.20.1-release.jar
# Inside jar: assets/yes_steve_model/builtin/{model_dirs}/
```

Extract to: `src/main/resources/assets/ysm/models/builtin/`

### 4.2 Preserve directory structure
Each model directory contains:
```
{model_id}/
├── ysm.json              # Model metadata (name, author, license, spec, files)
├── models/
│   ├── main.json          # Bedrock geo.json (main body)
│   └── arm.json           # Bedrock geo.json (first-person arms)
├── animations/
│   ├── main.animation.json       # Base animations (idle, walk, run, sneak, swim...)
│   ├── arm.animation.json        # Arm animations
│   ├── extra.animation.json      # Expression animations (dance, wave, clap...)
│   ├── tac.animation.json        # TACZ gun compat (if present)
│   ├── slashblade.animation.json # SlashBlade compat (if present)
│   ├── tlm.animation.json        # TLM maid compat (if present)
│   ├── carryon.animation.json    # CarryOn compat (if present)
│   ├── parcool.animation.json    # Parcool compat (if present)
│   └── swem.animation.json       # SWEM horse compat (if present)
├── textures/
│   ├── default.png        # Default skin
│   ├── gsl.png            # Skin variant
│   ├── hl.png             # Skin variant
│   └── ...                # More skin variants
├── avatar/                # Preview avatar (if present)
└── lang/
    ├── zh_cn.json         # Chinese name/description
    └── en_us.json         # English name/description
```

### 4.3 Create extraction script
Python script to:
1. Open `ysm-2.6.2-forge+mc1.20.1-release.jar` as a zip
2. List all files under `assets/yes_steve_model/builtin/`
3. Extract to target directory preserving structure
4. Verify all expected files are present (ysm.json, models/*.json, animations/*.json, textures/*.png)

### 4.4 Verify extracted resources
- All 19 models have valid `ysm.json` with parseable metadata
- All geo.json files are valid Bedrock format
- All PNG textures are valid images
- All animation.json files are valid Bedrock animation format

## Verification
- [ ] All 19 models extracted to correct directory
- [ ] All ysm.json files parse correctly
- [ ] GeckoLib 4 can load any extracted model's geo.json
- [ ] All 84+ skin textures are valid PNG files
- [ ] Animation files parse correctly
