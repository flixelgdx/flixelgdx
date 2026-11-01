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

import org.flixelgdx.graphics.wgsl.FlixelWgsl;
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
        org.flixelgdx.graphics.wgsl.FlixelWgslLayout.parseStruct(pre, "FlixelGlobals").getSize());
  }

  @Test
  void assemblePutsThePreambleFirst() {
    String user = "@fragment fn fs_main(in: FlixelVertexOutput) -> @location(0) vec4f { return in.color; }";
    String full = FlixelWgsl.assemble(user);
    assertTrue(full.startsWith(FlixelWgsl.getPreamble()));
    assertTrue(full.endsWith(user));
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
