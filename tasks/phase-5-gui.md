# Phase 5: Model Selection GUI

## Objective
Create an in-game GUI screen for selecting models and skin variants, plus keybind-based animation triggers.

## Context
YSM has several GUI screens for model management. We simplify this to: (1) a model selection screen accessible via keybind, (2) skin variant switching, and (3) extra animation triggers via keybinds. The original has 2,157 LOC in GUI code across 6 screens — we aim for ~300-400 LOC.

## Tasks

### 5.1 Model selection screen
Create `YSMModelScreen` extending Minecraft's `Screen`:
- Display a scrollable list of all 19 available models
- Show model name (from lang file), author, license
- Show preview of each model (or at least the skin texture thumbnail)
- Click to select → save to player's capability data
- Close to apply

Reference (simplified from): `/Users/neil/Code/guion-opensource/YesSteveModel/src/main/java/com/elfmcys/yesstevemodel/client/gui/ModelManageScreen.java` (324 LOC)

### 5.2 Skin variant selector
When a model is selected, allow cycling through its skin variants:
- Action wheel (right-click with special item) OR simple keybind cycle
- Shows available skin variants from the model's textures directory
- Saves selected variant to player's capability

Reference: `/Users/neil/Code/guion-opensource/YesSteveModel/src/main/java/com/elfmcys/yesstevemodel/client/gui/PlayerTextureScreen.java` (291 LOC)

### 5.3 Keybind registration
Register keybinds:
- **Open model screen**: Default key `Y` (configurable)
- **Cycle skin variant**: Default key `Y` + modifier (e.g., Shift+Y)
- **Animation wheel/trigger**: Default key `Z` (configurable)
  - Can be simplified to: press Z cycles through extra animations

Reference: `/Users/neil/Code/guion-opensource/YesSteveModel/src/main/java/com/elfmcys/yesstevemodel/client/input/` (186 LOC, 5 files)

### 5.4 Save/load player model selection
Use Forge Capability system to persist:
- Which model the player has selected (model ID string)
- Which skin variant they've chosen (texture name string)
- This data persists across game sessions

Reference: `/Users/neil/Code/guion-opensource/YesSteveModel/src/main/java/com/elfmcys/yesstevemodel/capability/` (371 LOC, 6 files)

## Reference Files
- GUI screens: `/Users/neil/Code/guion-opensource/YesSteveModel/src/main/java/com/elfmcys/yesstevemodel/client/gui/`
- Input handling: `/Users/neil/Code/guion-opensource/YesSteveModel/src/main/java/com/elfmcys/yesstevemodel/client/input/`
- Capability system: `/Users/neil/Code/guion-opensource/YesSteveModel/src/main/java/com/elfmcys/yesstevemodel/capability/`

## Verification
- [ ] Press Y key opens model selection screen
- [ ] All 19 models listed with names (English or Chinese)
- [ ] Selecting a model changes player's rendered model
- [ ] Skin variant cycling works (Shift+Y or similar)
- [ ] Model selection persists after game restart
- [ ] Other players see the correct model (if they also have the mod)
