package com.elfmcys.yesstevemodel.client.molang

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MolangRewriterTest {

    @Test
    fun `simple expressions pass through unchanged`() {
        assertEquals("0", MolangRewriter.rewrite("0"))
        assertEquals("query.anim_time", MolangRewriter.rewrite("query.anim_time"))
        assertEquals("math.cos(q.anim_time*1440)", MolangRewriter.rewrite("math.cos(q.anim_time*1440)"))
    }

    @Test
    fun `simple variable assignment becomes zero`() {
        assertEquals("0", MolangRewriter.rewrite("v.s=24"))
        assertEquals("0", MolangRewriter.rewrite("v.bv=math.lerp(20,math.cos(q.anim_time),5)"))
    }

    @Test
    fun `multi-statement keeps only the last non-assignment`() {
        // All assignments -> 0
        assertEquals("0", MolangRewriter.rewrite("v.a=1;v.b=2;v.c=3;"))
        // Last is non-assignment expression
        assertEquals("query.anim_time", MolangRewriter.rewrite("v.a=1;v.b=2;query.anim_time"))
    }

    @Test
    fun `custom namespaces become zero`() {
        assertEquals("0", MolangRewriter.rewrite("ctrl.elytra_fly"))
        assertEquals("0+(0)", MolangRewriter.rewrite("ysm.head_pitch+(ctrl.is_jumping)"))
    }

    @Test
    fun `boolean literal comparisons normalize`() {
        assertEquals("0!=0", MolangRewriter.rewrite("ctrl.elytra_fly==true"))
        assertEquals("(0!=0?-15:0)+(query.head_y_rotation)",
            MolangRewriter.rewrite("(ctrl.elytra_fly==true?-15:0)+(query.head_y_rotation)"))
    }

    @Test
    fun `complex YSM cape physics line is reduced cleanly`() {
        // Real example from wine_fox/02_new_year main.animation.json
        val input = "v.L1_K3=v.L1_R*v.L1_C/2/math.pi/v.L1_F;"
        val output = MolangRewriter.rewrite(input)
        // Result should be parseable; we don't insist on a particular value, just no errors
        assertEquals("0", output)
    }

    @Test
    fun `equality comparison without true-false is preserved`() {
        // Don't break existing valid expressions
        val input = "query.anim_time==1.5"
        val output = MolangRewriter.rewrite(input)
        // No assignment, no custom ns, no true/false -> unchanged
        assertEquals(input, output)
    }
}
