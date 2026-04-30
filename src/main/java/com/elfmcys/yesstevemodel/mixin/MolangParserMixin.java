package com.elfmcys.yesstevemodel.mixin;

import com.elfmcys.yesstevemodel.client.molang.MolangRewriter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import software.bernie.geckolib.core.molang.MolangException;
import software.bernie.geckolib.core.molang.MolangParser;
import software.bernie.geckolib.core.molang.expressions.MolangValue;

/**
 * Pre-processes molang expression strings before {@link MolangParser#parseExpression} runs.
 * The YSM 2.6.x animation files use molang extensions (variable assignment as expression,
 * custom namespaces, multi-statement programs) that GeckoLib's stock parser rejects. The
 * rewriter in {@link MolangRewriter} simplifies them into something parseable; this mixin
 * is the entry point that gives every expression that chance.
 *
 * <p>A re-entry guard avoids infinite recursion since this mixin's body itself calls back
 * into {@code parseExpression}. The recursive call hits the original implementation.
 */
@Mixin(value = MolangParser.class, remap = false)
public class MolangParserMixin {

    private static final ThreadLocal<Boolean> ysm$rewriting = ThreadLocal.withInitial(() -> false);

    @Inject(
        method = "parseExpression(Ljava/lang/String;)Lsoftware/bernie/geckolib/core/molang/expressions/MolangValue;",
        at = @At("HEAD"),
        cancellable = true,
        require = 0,
        remap = false
    )
    private static void ysm$rewriteBeforeParse(String expression, CallbackInfoReturnable<MolangValue> cir) throws MolangException {
        if (ysm$rewriting.get()) return; // recursive call from rewrite path; let original run
        String rewritten = MolangRewriter.INSTANCE.rewrite(expression);
        if (rewritten.equals(expression)) return; // no rewrite happened, defer to original parser

        ysm$rewriting.set(true);
        try {
            try {
                cir.setReturnValue(MolangParser.parseExpression(rewritten));
            } catch (MolangException primary) {
                // Rewritten form still won't parse (often a YSM expression with malformed
                // ternary or unsupported operator). Fall back to a zero constant so the
                // surrounding animation file finishes loading rather than aborting on this
                // single bad expression.
                try {
                    cir.setReturnValue(MolangParser.parseExpression("0"));
                } catch (MolangException secondary) {
                    // "0" failing means GeckoLib's parser itself is broken — surface the
                    // original failure by NOT cancelling, so the original code path runs.
                }
            }
        } finally {
            ysm$rewriting.set(false);
        }
    }
}
