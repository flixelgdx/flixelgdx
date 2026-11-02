/*
 * MIT License
 *
 * Copyright (c) 2026 stringdotjar
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.flixelgdx.graphics;

import org.flixelgdx.backend.gpu.wgsl.FlixelWgsl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelWgslTest {

  @Test
  void thePreambleIsOnTheClasspathAndDeclaresTheContract() {
    String pre = FlixelWgsl.getPreamble();
    assertTrue(pre.contains("struct FlixelGlobals"));
    assertTrue(pre.contains("@group(0) @binding(0) var<uniform> flixel"));
    assertTrue(pre.contains("@group(0) @binding(1) var flixelTexture"));
    assertTrue(pre.contains("@group(0) @binding(2) var flixelSampler"));
    assertTrue(pre.contains("struct FlixelVertexInput"));
    assertTrue(pre.contains("struct FlixelVertexOutput"));
    assertTrue(pre.contains("fn " + FlixelWgsl.DEFAULT_VERTEX_ENTRY));
  }

  @Test
  void theGlobalsSizeMatchesTheWgslLayout() {
    String pre = FlixelWgsl.getPreamble();
    assertEquals(FlixelWgsl.GLOBALS_SIZE,
        org.flixelgdx.backend.gpu.wgsl.FlixelWgslLayout.parseStruct(pre, "FlixelGlobals").getSize());
  }

  @Test
  void assemblePutsThePreambleFirstAndAddsThePremultiplyingEntryPoint() {
    String user = "@fragment fn fs_main(in: FlixelVertexOutput) -> @location(0) vec4f { return in.color; }";
    String full = FlixelWgsl.assemble(user);
    assertTrue(full.startsWith(FlixelWgsl.getPreamble()));
    String body = full.substring(FlixelWgsl.getPreamble().length() + 1);
    // The attributes are blanked in place, so every character after them keeps its column.
    assertTrue(
        body.startsWith("          fn fs_main(in: FlixelVertexOutput) ->              vec4f { return in.color; }"),
        body);
    assertTrue(body.contains("override " + FlixelWgsl.DARKEN_CONSTANT + ": bool = false;"));
    assertTrue(body.contains("fn " + FlixelWgsl.PREMULTIPLIED_FRAGMENT_ENTRY
        + "(in: FlixelVertexOutput) -> @location(0) vec4f {"));
    assertTrue(body.contains("let color = fs_main(in);"));
    assertEquals(FlixelWgsl.PREMULTIPLIED_FRAGMENT_ENTRY, FlixelWgsl.fragmentEntryOf(user));
  }

  @Test
  void theRewriteKeepsEveryLineInPlace() {
    String user = """
        // A comment first.
        struct Params { amount: f32, }
        @group(1) @binding(0) var<uniform> params: Params;

        @fragment
        fn fs_main(
            in: FlixelVertexOutput,
        ) -> @location(0) vec4<f32> {
          return in.color * params.amount;
        }
        """;
    String wrapped = FlixelWgsl.assemble(user).substring(FlixelWgsl.getPreamble().length() + 1);
    String[] before = user.split("\n", -1);
    String[] after = wrapped.split("\n", -1);
    for (int i = 0; i < before.length - 1; i++) {
      assertEquals(before[i].length(), after[i].length(), "line " + (i + 1));
    }
    assertEquals("  return in.color * params.amount;", after[8]);
    assertTrue(wrapped.contains("let color = fs_main(in);"));
  }

  @Test
  void parameterAttributesMoveToTheGeneratedEntryPoint() {
    String user = "@fragment fn fs_main(@builtin(position) pos: vec4f, in: FlixelVertexOutput) -> @location(0) vec4f"
        + " { return in.color; }";
    String wrapped = FlixelWgsl.assemble(user);
    assertTrue(wrapped.contains("fn fs_main(                   pos: vec4f, in: FlixelVertexOutput)"), wrapped);
    assertTrue(wrapped.contains("(@builtin(position) pos: vec4f, in: FlixelVertexOutput) -> @location(0) vec4f {"));
    assertTrue(wrapped.contains("let color = fs_main(pos, in);"));
  }

  @Test
  void aFragmentStageThatIsNotAPlainVec4fIsLeftAlone() {
    String user = "struct Out { @location(0) c: vec4f, }\n"
        + "@fragment fn fs_main(in: FlixelVertexOutput) -> Out { var o: Out; o.c = in.color; return o; }";
    assertEquals(FlixelWgsl.getPreamble() + '\n' + user, FlixelWgsl.assemble(user));
    assertEquals(FlixelWgsl.FRAGMENT_ENTRY, FlixelWgsl.fragmentEntryOf(user));
    assertEquals(FlixelWgsl.FRAGMENT_ENTRY, FlixelWgsl.fragmentEntryOf("fn helper() {}"));
  }

  @Test
  void aShaderWithoutVsMainUsesTheDefaultVertexStage() {
    assertEquals(FlixelWgsl.DEFAULT_VERTEX_ENTRY, FlixelWgsl.vertexEntryOf("@fragment fn fs_main() {}"));
  }

  @Test
  void aShaderWithVsMainUsesIt() {
    String src =
        "@vertex fn vs_main(in: FlixelVertexInput) -> FlixelVertexOutput { var o: FlixelVertexOutput; return o; }";
    assertEquals(FlixelWgsl.VERTEX_ENTRY, FlixelWgsl.vertexEntryOf(src));
  }

  @Test
  void aCommentedOutVsMainDoesNotCount() {
    assertFalse(FlixelWgsl.definesFunction("// fn vs_main() {}\n/* fn vs_main */ fn other() {}", "vs_main"));
    assertTrue(FlixelWgsl.definesFunction("fn   vs_main (a: f32) {}", "vs_main"));
    assertFalse(FlixelWgsl.definesFunction("fn vs_main2() {}", "vs_main"));
  }
}
