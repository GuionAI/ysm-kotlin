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
        if (ysm$rewriting.get()) return; // recursive call from the rewrite path; let original run
        String rewritten = MolangRewriter.INSTANCE.rewrite(expression);
        if (rewritten.equals(expression)) return; // nothing to change
        ysm$rewriting.set(true);
        try {
            cir.setReturnValue(MolangParser.parseExpression(rewritten));
        } catch (MolangException ignored) {
            // Rewritten form still failed — fall through to the original parser, which
            // will throw the same exception and surface in the GeckoLib loader log.
        } finally {
            ysm$rewriting.set(false);
        }
    }
}
