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
import org.flixelgdx.math.FlixelMatrix;
import org.flixelgdx.math.FlixelVector;
import org.flixelgdx.util.FlixelColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelUniformBufferTest {

  private static final String SOURCE = """
      struct U {
        time: f32,
        count: i32,
        offset: vec2f,
        tint: vec4f,
        axis: vec3f,
        flags: vec2<u32>,
        view: mat4x4f,
        small: mat2x2f,
      }
      @group(1) @binding(0) var<uniform> u: U;
      """;

  private static FlixelUniformBuffer create() {
    return new FlixelUniformBuffer(FlixelWgslLayout.parse(SOURCE));
  }

  private static float floatAt(FlixelUniformBuffer b, int offset) {
    return Float.intBitsToFloat(intAt(b, offset));
  }

  private static int intAt(FlixelUniformBuffer b, int offset) {
    byte[] d = b.getData();
    return (d[offset] & 0xFF) | ((d[offset + 1] & 0xFF) << 8) | ((d[offset + 2] & 0xFF) << 16)
        | ((d[offset + 3] & 0xFF) << 24);
  }

  @Test
  void floatsLandAtTheirOffsets() {
    FlixelUniformBuffer b = create();
    b.setFloat("time", 2.5f);
    assertEquals(2.5f, floatAt(b, 0));
  }

  @Test
  void integersAreStoredAsIntegersAndConvertedForFloatMembers() {
    FlixelUniformBuffer b = create();
    b.setInt("count", 7);
    assertEquals(7, intAt(b, 4));
    b.setFloat("count", 9.9f);
    assertEquals(9, intAt(b, 4));
    b.setInt("time", 3);
    assertEquals(3f, floatAt(b, 0));
  }

  @Test
  void vectorsColorsAndComponentsAreWritten() {
    FlixelUniformBuffer b = create();
    b.setVector("offset", new FlixelVector(1f, 2f));
    assertEquals(1f, floatAt(b, 8));
    assertEquals(2f, floatAt(b, 12));
    b.setColor("tint", new FlixelColor(0.1f, 0.2f, 0.3f, 0.4f));
    assertEquals(0.1f, floatAt(b, 16));
    assertEquals(0.4f, floatAt(b, 28));
    b.set("axis", 1f, 2f, 3f);
    assertEquals(1f, floatAt(b, 32));
    assertEquals(3f, floatAt(b, 40));
    b.set("tint", 5f, 6f, 7f, 8f);
    assertEquals(8f, floatAt(b, 28));
  }

  @Test
  void colorsFillAVec3WithoutOverflowing() {
    FlixelUniformBuffer b = create();
    b.setColor("axis", new FlixelColor(0.5f, 0.25f, 0.125f, 1f));
    assertEquals(0.125f, floatAt(b, 40));
    // The float after axis (flags, which starts at 48) must be untouched by the alpha.
    assertEquals(0, intAt(b, 44));
  }

  @Test
  void unsignedVectorsTakeIntegerComponents() {
    FlixelUniformBuffer b = create();
    b.set("flags", 3f, 4f);
    assertEquals(3, intAt(b, 48));
    assertEquals(4, intAt(b, 52));
  }

  @Test
  void aFullMatrixIsCopiedColumnMajor() {
    FlixelUniformBuffer b = create();
    FlixelMatrix m = new FlixelMatrix();
    for (int i = 0; i < 16; i++) {
      m.val[i] = i + 1;
    }
    b.setMatrix("view", m);
    int base = 64;
    for (int i = 0; i < 16; i++) {
      assertEquals(i + 1, floatAt(b, base + i * 4), "element " + i);
    }
  }

  @Test
  void aSmallerMatrixTakesTheUpperLeftPart() {
    FlixelUniformBuffer b = create();
    FlixelMatrix m = new FlixelMatrix();
    for (int i = 0; i < 16; i++) {
      m.val[i] = i + 1;
    }
    b.setMatrix("small", m);
    int base = 128;
    assertEquals(1f, floatAt(b, base));
    assertEquals(2f, floatAt(b, base + 4));
    assertEquals(5f, floatAt(b, base + 8));
    assertEquals(6f, floatAt(b, base + 12));
  }

  @Test
  void unknownNamesAreIgnoredAndNeverDirty() {
    FlixelUniformBuffer b = create();
    b.clearDirty();
    b.setFloat("missing", 1f);
    b.set("missing", 1f, 2f);
    b.setColor("missing", FlixelColor.RED);
    assertFalse(b.isDirty());
    assertFalse(b.has("missing"));
    assertTrue(b.has("time"));
  }

  @Test
  void onlyRealChangesMarkTheBufferDirty() {
    FlixelUniformBuffer b = create();
    assertTrue(b.isDirty(), "a new buffer must be uploaded once");
    b.clearDirty();
    b.setFloat("time", 0f);
    assertFalse(b.isDirty(), "writing the same value is not a change");
    b.setFloat("time", 1f);
    assertTrue(b.isDirty());
    b.clearDirty();
    b.setFloat("time", 1f);
    assertFalse(b.isDirty());
  }

  @Test
  void theBufferIsAsLargeAsTheStruct() {
    FlixelUniformBuffer b = create();
    assertEquals(b.getLayout().getSize(), b.getSize());
    assertEquals(144, b.getSize());
  }
}
