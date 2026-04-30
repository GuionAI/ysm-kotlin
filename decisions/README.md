# Decisions

Plain-language design notes for the choices made on ysm-kotlin. Written so a reader who hasn't done Minecraft mod development can follow.

Each note answers four questions:
1. **What's the problem?** — what we were trying to do and what was in the way
2. **What did we consider?** — alternatives, briefly
3. **What did we pick, and why?** — the actual decision and the load-bearing reason
4. **What does this mean later?** — consequences for future work

## Index

| # | Title | One-line summary |
|---|---|---|
| 01 | [Why GeckoLib 4](01-why-geckolib-4.md) | Use the modern external library instead of porting the original's embedded fork |
| 02 | [Build stack](02-build-stack.md) | Kotlin 2.2.21, KFF 4.12, GeckoLib 4.8.2 — and the version pitfalls each had |
| 03 | [Target modpack defines compat](03-target-modpack-and-compat.md) | 落幕曲 1.6.3 is the goalpost; its 263 mods set what "compatible" means |
| 04 | [Player renderer strategy](04-player-renderer-strategy.md) | Mixin into `LivingEntityRenderer.render`, don't replace `PlayerRenderer`. The big one. |
| 05 | [Armor deferred to Phase 7](05-armor-deferred.md) | Functional vs cosmetic — armor visuals are wrong-but-working, ship later |
| 06 | [ysm.json schema realities](06-ysm-json-schema.md) | What the real spec-2 format actually looks like vs the simplified plan |
| 07 | [Animation architecture](07-animation-architecture.md) | YSM fidelity vs vanilla-pose compat — the dual-source problem and 5 options |

## Status conventions

- **Locked** — implementation depends on this; changing it means rework
- **Tentative** — best current guess, may shift after first runClient
- **Superseded** — kept for history; see linked successor

## How these relate to `tasks/`

`tasks/phase-*.md` are the execution plan: *what* to build and in what order. `decisions/` is *why* — the reasoning that shaped the plan. When the two disagree, decisions win and the task file gets updated to match.
