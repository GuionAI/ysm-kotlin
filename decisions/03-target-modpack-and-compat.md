# 03 — The target modpack defines our compatibility envelope

**Status:** Locked

## What's the problem?

A Minecraft mod isn't an app. It's a piece of code injected into a running game alongside potentially hundreds of other mods, all sharing the same `PlayerRenderer`, `HumanoidModel`, animation hooks, etc. Two mods that both want to "modify how the player is drawn" can fight, race, or silently override each other.

So "does our mod work?" isn't a yes/no — it's "does our mod work *together with the specific other mods players will install it next to*?"

Without a concrete compat target, every architectural decision is a guess. With one, every decision has a check.

## What did we pick, and why?

The compat target is **`落幕曲 1.6.3`** (Curtain Call 1.6.3), a Chinese Forge modpack at:
```
~/Downloads/落幕曲/整合包主体  安装包/落幕曲1.6.3安装包（拖入启动器安装）.zip
```

Manifest (CurseForge installer format):
- Minecraft 1.20.1
- Forge 47.3.22
- 263 mods total

Why this one: it's what users will run our mod inside. It also happens to include every category of player-render-touching mod that could break us, so passing it is a strong signal.

## Mods that touch the player render (must keep working)

| Family | Count | What they do |
|---|---|---|
| **TACZ** (Timeless and Classics Zero) + 4 addons | 5 | Gun mod. Aiming/recoil animations on the player's arms. Uses playerAnimator. |
| **SlashBlade** + 3 addons | 4 | Sword mod. Swing/charge stances mutate `HumanoidModel` arm bones. |
| **playerAnimator** (KosmX) | 1 | The shared animation framework that TACZ and SlashBlade both depend on. Hooks into vanilla `PlayerRenderer`, `PlayerModel`, `HumanoidModel` directly via mixins. |
| **Combat Nouveau, Cataclysm Try Hard, SW's wukong, Meet Your Fight, Horse Combat Controls** | 5 | Various combat-stance animations on the player. |
| **Entity Model Features** | 1 | Texture/model patcher. May interact with player skin. |
| **Kotlin for Forge** | 1 | Same dep we already use. ✓ |

Notable absences:
- **No `yes_steve_model`** — the original mod isn't in the pack, so our mod ID `yesstevemodel` (without underscore) doesn't conflict.
- **No standalone GeckoLib or AzureLib** — TACZ may bundle its own animation runtime; we add GeckoLib 4 ourselves.
- **No FirstPerson mod** — first-person work in Phase 7 is unconstrained.
- **No NotEnoughAnimations** — Phase 7's NEA-compat task may be unnecessary in practice.

## What does this mean later?

This single fact — playerAnimator's mixins are bound to `PlayerRenderer` and `PlayerModel` *by class* — drove the most consequential architectural decision in the project. See [04-player-renderer-strategy.md](04-player-renderer-strategy.md). Without the modpack analysis we'd likely have copied the original YSM 1.1.5's approach (full custom renderer) and silently shipped a build that breaks every TACZ aim animation in the pack.

**How to use this in practice:**
- After every Phase-2-or-later change, drop the built jar into `~/Code/guion-opensource/minecraft/.minecraft/mods/` and visually verify TACZ aim + SlashBlade swing both still drive the YSM bedrock model.
- Any architectural choice that *can't* satisfy this constraint (e.g. registering a separate `EntityRenderer<Player>`) is rejected on those grounds, not on aesthetic ones.
- The modpack is the spec. If 落幕曲 ships a 1.7.x with new mods, we re-evaluate.
