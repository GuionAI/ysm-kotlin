package com.elfmcys.yesstevemodel.client.molang

/**
 * Rewrites YSM-extension molang into a GeckoLib-compatible subset.
 *
 * The YSM 2.6.x animation files contain imperative molang programs that use:
 *   - Variable assignment as expression: `v.s=24`, `v.bv=math.lerp(...)`
 *   - Custom namespaces: `ctrl.elytra_fly`, `ysm.head_pitch` (animation-controller-
 *     state and YSM-specific queries that the original mod backed with custom variables)
 *   - Multi-statement programs separated by `;`: `v.a=1; v.b=v.a*2; v.c=v.b/3`
 *   - Boolean literal comparison: `ctrl.elytra_fly==true`
 *
 * GeckoLib 4's stock molang parser rejects all of the above, so every animation
 * containing any such expression fails to load — including basic idle/walk/run
 * keyframe-only animations sharing a file with scripted ones, because a single
 * parse error aborts the whole `BakedAnimations` deserialize call.
 *
 * Rewrite policy here is **aggressive simplification** rather than full fidelity:
 *   1. Drop variable assignments — replace `v.x=expr` with `0`. The cape/hair physics
 *      simulations break (their state never advances), but keyframe-driven
 *      animations recover fully.
 *   2. Replace `ctrl.X` and `ysm.X` references with `0`. We don't have the
 *      animation-controller state; without state, treating these as zero is the
 *      cheapest "I don't know" answer that still parses.
 *   3. Normalize `==true` / `==false` to `!=0` / `==0` so any custom boolean
 *      comparisons survive.
 *   4. Multi-statement programs: keep only the last expression after the last `;`.
 *      If the last statement is an assignment, drop it (yields `0`).
 *
 * Phase 3b first cut. A future phase that actually wants the YSM physics will need
 * a real molang interpreter — most likely a separate dependency or a hand-rolled
 * one wired around eliotlash/molang. For now, recover the animation key set, and
 * accept loss of secondary procedural effects.
 */
object MolangRewriter {

    private val customNamespacePattern = Regex("""\b(?:ctrl|ysm)\.\w+""", RegexOption.IGNORE_CASE)
    private val booleanTrue = Regex("""==\s*true\b""", RegexOption.IGNORE_CASE)
    private val booleanFalse = Regex("""==\s*false\b""", RegexOption.IGNORE_CASE)
    private val assignmentStatement = Regex("""\b(?:v|variable|t|temp)\.\w+\s*=\s*[^;]*?(?=;|$)""", RegexOption.IGNORE_CASE)

    /**
     * Returns a rewritten expression string compatible with GeckoLib's stock parser.
     * Returns the original input unchanged if no rewrites apply.
     */
    fun rewrite(expression: String): String {
        if (!needsRewriting(expression)) return expression

        var result = expression

        // Handle multi-statement programs: split on ';', drop assignments, keep last
        // non-empty non-assignment statement.
        if (result.contains(';')) {
            result = simplifyMultiStatement(result)
        }

        // Single-statement: drop a top-level assignment to leave behind 0.
        result = assignmentStatement.replace(result, "0")

        // Custom namespaces -> 0.
        result = customNamespacePattern.replace(result, "0")

        // Boolean literals.
        result = booleanTrue.replace(result, "!=0")
        result = booleanFalse.replace(result, "==0")

        // Strip trailing semicolons that may have survived simplification.
        result = result.trim().trimEnd(';').trim()
        if (result.isEmpty()) result = "0"

        return result
    }

    private fun needsRewriting(s: String): Boolean =
        s.contains(';') ||
            s.contains('=') && !s.contains("==") && !s.contains(">=") && !s.contains("<=") && !s.contains("!=") ||
            customNamespacePattern.containsMatchIn(s) ||
            "true" in s || "false" in s ||
            // assignments using == comparison handled separately, look for bare assignment
            Regex("""[a-zA-Z]\.\w+\s*=[^=]""").containsMatchIn(s)

    private fun simplifyMultiStatement(program: String): String {
        val statements = program.split(';')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        // Walk statements in reverse; first non-assignment is our keeper.
        for (stmt in statements.asReversed()) {
            if (!isAssignment(stmt)) return stmt
        }
        // All statements were assignments — fall back to 0.
        return "0"
    }

    private fun isAssignment(s: String): Boolean =
        Regex("""^\s*(?:v|variable|t|temp)\.\w+\s*=[^=]""", RegexOption.IGNORE_CASE).containsMatchIn(s)
}
