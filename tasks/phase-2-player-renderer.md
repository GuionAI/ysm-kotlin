# Phase 2: Player Renderer Replacement

## Objective
Replace the vanilla PlayerRenderer with a GeckoLib 4-based renderer that draws custom Bedrock models instead of Steve/Alex.

## Context
This is the **most critical phase** and the **hardest part of the entire project**. GeckoLib 4 does NOT have a built-in way to replace the Player entity renderer — `GeoReplacedEntityRenderer` is designed for other entity types, not Player. The original YSM 1.1.5 solved this by embedding a full copy of GeckoLib 3 with custom modifications (6,724 LOC). Forum posts and community mods (e.g., Shipwrecker's content on Forge Forums) confirm that **mixin injection** is required to hook into the Player rendering pipeline.

### Two approaches (worker should evaluate both and pick one):

**Approach A: Mixin into PlayerRenderer (recommended)**
- Inject at `PlayerRenderer.render()` or `HumanoidModel.setupAnim()` to override the model
- Use GeckoLib's `GeoModel` to load and render the Bedrock model
- Keep the vanilla PlayerRenderer shell but replace its model
- This is closest to what the original 1.1.5 `GeoReplacedEntityRenderer` subclass did

**Approach B: Register custom renderer via EntityRenderersEvent**
- Use `EntityRenderersEvent.RegisterRenderers` to replace the Player renderer entirely
- Implement a full `EntityRenderer<Player>` that delegates to GeckoLib rendering
- More complete replacement but more code to write

### Key insight from original source
The 1.1.5 code has a `CustomPlayerEntity` (143 LOC) that wraps the real Player as a `GeoEntity`. This proxy entity feeds GeckoLib's rendering pipeline the correct model/texture path. We likely need the same pattern.

## Tasks

### 2.1 Create custom player entity
Create a `YSMPlayerEntity` that implements `GeoEntity`:
- This is a wrapper/proxy entity used by GeckoLib's renderer system
- It holds a reference to the actual player and the currently selected model
- Implements `GeoEntity.getGeoModelLocation()` to return the current model's geo.json path
- Implements `GeoEntity.getTextureLocation()` to return the current skin variant texture

Reference: `/Users/neil/Code/guion-opensource/YesSteveModel/src/main/java/com/elfmcys/yesstevemodel/client/entity/CustomPlayerEntity.java` (143 LOC)

### 2.2 Create GeckoLib player renderer
Create `YSMPlayerRenderer` that replaces the vanilla PlayerRenderer:

**Important**: GeckoLib 4's `GeoEntityRenderer` is for custom entities, not for replacing the Player renderer directly. You need to either:
- (A) Write a mixin that injects into `PlayerRenderer` to swap the model being rendered, OR
- (B) Write a custom `EntityRenderer<Player>` that internally uses GeckoLib's rendering methods

The original 1.1.5 code used GeckoLib 3's `GeoReplacedEntityRenderer` class, which was specifically designed for this. GeckoLib 4 may have a different approach — check the [GeckoLib 4 Forge examples repo](https://github.com/bernie-g/geckolib-examples) for any player replacement examples.

Reference: `/Users/neil/Code/guion-opensource/YesSteveModel/src/main/java/com/elfmcys/yesstevemodel/client/renderer/CustomPlayerRenderer.java` (154 LOC)
Also see: GeckoLib wiki https://github.com/bernie-g/geckolib/wiki/Geckolib-Entities-(Geckolib4)

### 2.3 Register the renderer via Forge events or Mixin
- **Approach A (Mixin)**: Inject into `PlayerRenderer` to replace the `HumanoidModel` with a GeckoLib-backed model during `setupAnim()` and `render()`
- **Approach B (Event)**: Use `EntityRenderersEvent.RegisterRenderers` to replace the Player renderer entirely
- This MUST happen during mod initialization, before any players join

Note: The original code used `EntityRenderersEvent` (Approach B). Check if GeckoLib 4 supports this for Player entities. If not, Mixin (Approach A) is required.

Reference: `/Users/neil/Code/guion-opensource/YesSteveModel/src/main/java/com/elfmcys/yesstevemodel/client/event/RegisterEntityRenderersEvent.java`

### 2.4 Layer support
The original YSM renderer has special layers for:
- **Elytra rendering**: Player's elytra should still render when wearing one
- **Item in hand**: Held items should still appear

Reference:
- `/Users/neil/Code/guion-opensource/YesSteveModel/src/main/java/com/elfmcys/yesstevemodel/client/renderer/layer/CustomPlayerElytraLayer.java`
- `/Users/neil/Code/guion-opensource/YesSteveModel/src/main/java/com/elfmcys/yesstevemodel/client/renderer/layer/CustomPlayerItemInHandLayer.java`

### 2.5 Vanilla model hiding
When YSM model is active, hide the vanilla player model parts that would visually overlap. This should be done per-part (head, body, arms, legs) to allow partial model replacement if needed.

## Reference Files
- Renderer: `/Users/neil/Code/guion-opensource/YesSteveModel/src/main/java/com/elfmcys/yesstevemodel/client/renderer/CustomPlayerRenderer.java`
- Entity: `/Users/neil/Code/guion-opensource/YesSteveModel/src/main/java/com/elfmcys/yesstevemodel/client/entity/CustomPlayerEntity.java`
- Model class: `/Users/neil/Code/guion-opensource/YesSteveModel/src/main/java/com/elfmcys/yesstevemodel/client/model/CustomPlayerModel.java`
- Event registration: `/Users/neil/Code/guion-opensource/YesSteveModel/src/main/java/com/elfmcys/yesstevemodel/client/event/RegisterEntityRenderersEvent.java`
- GeckoLib 4 Entity wiki: https://github.com/bernie-g/geckolib/wiki/Geckolib-Entities-(Geckolib4)
- GeckoLib 4 Examples: https://github.com/bernie-g/geckolib-examples
- Forge Forums (player replacement difficulty): https://forums.minecraftforge.net/topic/151927-1201-cant-figure-out-how-to-get-entityrendererprovidercontext-difficulty-getting-custom-player-model-to-render/

## Verification
- [ ] With a test model loaded, the player renders with the custom Bedrock model instead of Steve
- [ ] Vanilla model parts are properly hidden (no Steve head floating through the custom model)
- [ ] Armor still renders on top of the custom model
- [ ] Held items (sword, bow, etc.) appear in the model's hands
- [ ] Elytra renders on the custom model's back
