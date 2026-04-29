# 01 — Use GeckoLib 4 as an external dependency

**Status:** Locked

## What's the problem?

The thing this project replaces (Yes Steve Model, "YSM") needs to render custom 3D character models for the player. Those models come in **Bedrock Edition format** — JSON files describing a skeleton of `cubes` grouped into `bones`, plus separate JSON animation files. Vanilla Minecraft can't load this format.

Two parts are needed:
- A **parser/renderer** that turns Bedrock JSON into something OpenGL can draw
- An **animation engine** that interpolates keyframes per bone per frame

The original YSM 1.1.5 source we're using as reference is ~17,900 lines of Java. About 6,700 of those are an embedded copy of an animation library called **GeckoLib 3**, plus another ~1,800 of a math library called **MClib**. They forked GeckoLib 3 into the project tree and modified it directly because they needed to bend it to render the *Player* (which Minecraft draws in a special way) — and at the time, GeckoLib didn't support that out of the box.

## What did we consider?

| Option | Pros | Cons |
|---|---|---|
| Port GeckoLib 3 fork verbatim | Closest to what worked before | ~6.7k LOC of nontrivial code we don't really understand; carries any bugs the original had |
| Write our own engine from scratch | Total control | Months of work; reinvents a solved problem |
| Use **GeckoLib 4** as an external dependency | Same code path as every other modern animated-mob mod; ~60% of the original codebase deletes | API surface is built around custom *entities*, not the Player. Requires extra glue to use for player rendering. |

## What did we pick, and why?

**GeckoLib 4 (`software.bernie.geckolib:geckolib-forge-1.20.1:4.8.2`) as an external dependency.**

The 60%-codebase reduction isn't the load-bearing reason — staying current is. GeckoLib 4 ships every month, fixes Bedrock-format edge cases on the live tree, and is the same library TACZ, SlashBlade, and many other mods consume. If we vendor a fork like the original did, we sign up to maintain it indefinitely.

The "doesn't natively support Player" downside is real but bounded — that's exactly what Phase 2 is for, and the glue layer is on the order of 200–300 LOC, not 6,700. See [04-player-renderer-strategy.md](04-player-renderer-strategy.md) for how that bridge works.

## What does this mean later?

- We never write Bedrock-JSON parsing code ourselves. Phase 1's data classes only describe `ysm.json` (the *manifest* that points at the geo files). The `.geo.json` files themselves are handed to GeckoLib 4 as `ResourceLocation`s and it does the rest.
- We never write keyframe interpolation, easing, or molang evaluation. GeckoLib 4 owns that pipeline.
- If GeckoLib 4 ships a 4.9.x with a breaking change, we update one version string and possibly fix a few API call-sites — vs the alternative of merging upstream into our fork.
- The work that *can't* go away: bridging vanilla Minecraft's player rendering pipeline to GeckoLib's animation pipeline. That's the entire substance of Phase 2 (and Phase 6 for mod compat).
