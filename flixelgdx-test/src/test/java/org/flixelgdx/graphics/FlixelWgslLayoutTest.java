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

import org.flixelgdx.backend.gpu.wgsl.FlixelWgslLayout;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelWgslLayoutTest {

  private static FlixelWgslLayout layoutOf(String struct) {
    return FlixelWgslLayout.parse(struct + "\n@group(1) @binding(0) var<uniform> u: U;\n");
  }

  private static int offsetOf(FlixelWgslLayout layout, String name) {
    int field = layout.find(name);
    assertTrue(field >= 0, "missing member " + name);
    return layout.getOffset(field);
  }

  @Test
  void scalarsAndVec4FollowEachOther() {
    FlixelWgslLayout l = layoutOf("struct U { time: f32, amount: f32, tint: vec4f, }");
    assertEquals(0, offsetOf(l, "time"));
    assertEquals(4, offsetOf(l, "amount"));
    assertEquals(16, offsetOf(l, "tint"));
    assertEquals(32, l.getSize());
  }

  @Test
  void vec3IsTwelveBytesWideButSixteenAligned() {
    FlixelWgslLayout l = layoutOf("struct U { first: f32, v: vec3f, after: f32 }");
    assertEquals(0, offsetOf(l, "first"));
    assertEquals(16, offsetOf(l, "v"));
    assertEquals(28, offsetOf(l, "after"));
    assertEquals(32, l.getSize());
  }

  @Test
  void vec2IsEightAligned() {
    FlixelWgslLayout l = layoutOf("struct U { a: f32, b: vec2f }");
    assertEquals(8, offsetOf(l, "b"));
    assertEquals(16, l.getSize());
  }

  @Test
  void explicitGenericFormsMatchTheShorthands() {
    FlixelWgslLayout l = layoutOf("struct U { a: vec3<f32>, m: mat4x4<f32>, n: vec2<i32>, k: vec4<u32> }");
    assertEquals(0, offsetOf(l, "a"));
    assertEquals(16, offsetOf(l, "m"));
    assertEquals(80, offsetOf(l, "n"));
    assertEquals(96, offsetOf(l, "k"));
    assertEquals(FlixelWgslLayout.I32, l.getScalar(l.find("n")));
    assertEquals(FlixelWgslLayout.U32, l.getScalar(l.find("k")));
    assertEquals(112, l.getSize());
  }

  @Test
  void matricesHavePaddedColumns() {
    FlixelWgslLayout l = layoutOf("struct U { m3: mat3x3f, m2: mat2x2f, tail: f32 }");
    assertEquals(0, offsetOf(l, "m3"));
    assertEquals(16, l.getColumnStride(l.find("m3")));
    assertEquals(48, offsetOf(l, "m2"));
    assertEquals(8, l.getColumnStride(l.find("m2")));
    assertEquals(64, offsetOf(l, "tail"));
    assertEquals(80, l.getSize());
  }

  @Test
  void arraysExpandIntoNamedElements() {
    FlixelWgslLayout l = layoutOf("struct U { lights: array<vec4f, 3>, count: i32 }");
    assertEquals(0, offsetOf(l, "lights[0]"));
    assertEquals(16, offsetOf(l, "lights[1]"));
    assertEquals(32, offsetOf(l, "lights[2]"));
    assertEquals(0, offsetOf(l, "lights"));
    assertEquals(48, offsetOf(l, "count"));
    assertEquals(64, l.getSize());
    assertEquals(-1, l.find("lights[3]"));
  }

  @Test
  void nestedStructsAreFlattenedWithDots() {
    FlixelWgslLayout l = layoutOf("""
        struct Light { color: vec3f, power: f32 }
        struct U { pos: vec3f, light: Light }
        """);
    assertEquals(0, offsetOf(l, "pos"));
    assertEquals(16, offsetOf(l, "light.color"));
    assertEquals(28, offsetOf(l, "light.power"));
    assertEquals(32, l.getSize());
  }

  @Test
  void arraysOfStructsUseTheStructStride() {
    FlixelWgslLayout l = layoutOf("""
        struct Item { a: vec2f, b: f32 }
        struct U { items: array<Item, 2> }
        """);
    assertEquals(0, offsetOf(l, "items[0].a"));
    assertEquals(8, offsetOf(l, "items[0].b"));
    assertEquals(16, offsetOf(l, "items[1].a"));
    assertEquals(24, offsetOf(l, "items[1].b"));
    assertEquals(32, l.getSize());
  }

  @Test
  void alignAndSizeAttributesAreHonored() {
    FlixelWgslLayout l = layoutOf("struct U { a: f32, @align(16) b: f32, @size(32) c: f32, d: f32 }");
    assertEquals(0, offsetOf(l, "a"));
    assertEquals(16, offsetOf(l, "b"));
    assertEquals(20, offsetOf(l, "c"));
    assertEquals(52, offsetOf(l, "d"));
    assertEquals(64, l.getSize());
  }

  @Test
  void aPlainUniformVariableIsOneMember() {
    FlixelWgslLayout l = FlixelWgslLayout.parse("@group(1) @binding(0) var<uniform> time: f32;");
    assertEquals(0, offsetOf(l, "time"));
    assertEquals(4, l.getSize());
  }

  @Test
  void attributesInEitherOrderAndOnSeparateLinesWork() {
    FlixelWgslLayout l = FlixelWgslLayout.parse("""
        struct U { v: f32 }
        @binding(0)
        @group(1)
        var<uniform> u : U;
        """);
    assertEquals(0, offsetOf(l, "v"));
  }

  @Test
  void commentsAreIgnored() {
    FlixelWgslLayout l = FlixelWgslLayout.parse("""
        // struct U { fake: f32 }
        /* @group(1) @binding(0) var<uniform> nope: f32; */
        struct U { /* inline */ real: f32, // trailing
          other: f32 }
        @group(1) @binding(0) var<uniform> u: U;
        """);
    assertEquals(-1, l.find("fake"));
    assertEquals(4, offsetOf(l, "other"));
  }

  @Test
  void otherBindingsAreNotTheShaderUniform() {
    FlixelWgslLayout l = FlixelWgslLayout.parse("""
        struct U { v: f32 }
        @group(0) @binding(0) var<uniform> framework: U;
        @group(1) @binding(1) var<uniform> second: U;
        """);
    assertTrue(l.isEmpty());
    assertEquals(0, l.getSize());
  }

  @Test
  void shadersWithoutUniformsHaveAnEmptyLayout() {
    FlixelWgslLayout l = FlixelWgslLayout.parse("@fragment fn fs_main() -> @location(0) vec4f { return vec4f(1.0); }");
    assertTrue(l.isEmpty());
    assertEquals(-1, l.find("anything"));
  }

  @Test
  void unsupportedTypesAreRejectedWithAMessage() {
    IllegalArgumentException f16 = assertThrows(IllegalArgumentException.class, () -> layoutOf("struct U { a: f16 }"));
    assertTrue(f16.getMessage().contains("f16"));
    assertThrows(IllegalArgumentException.class, () -> layoutOf("struct U { a: array<f32> }"));
    assertThrows(IllegalArgumentException.class, () -> layoutOf("struct U { a: Missing }"));
    assertThrows(IllegalArgumentException.class, () -> layoutOf("struct U { a: bool }"));
  }

  @Test
  void parseStructLaysOutANamedStruct() {
    FlixelWgslLayout l = FlixelWgslLayout.parseStruct("struct Params { a: f32, b: vec2f }", "Params");
    assertEquals(8, offsetOf(l, "b"));
  }
}
