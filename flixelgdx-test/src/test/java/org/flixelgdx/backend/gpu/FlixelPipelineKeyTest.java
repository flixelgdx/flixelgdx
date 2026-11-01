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
package org.flixelgdx.backend.gpu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FlixelPipelineKeyTest {

  @Test
  void partsRoundTrip() {
    long key = FlixelPipelineKey.of(7, 200, 123_456);
    assertEquals(7, FlixelPipelineKey.blendOf(key));
    assertEquals(200, FlixelPipelineKey.formatOf(key));
    assertEquals(123_456, FlixelPipelineKey.shaderOf(key));
  }

  @Test
  void extremesDoNotBleedIntoNeighbors() {
    long key = FlixelPipelineKey.of(FlixelPipelineKey.MAX_BLEND - 1, FlixelPipelineKey.MAX_FORMAT - 1,
        FlixelPipelineKey.MAX_SHADER - 1);
    assertEquals(FlixelPipelineKey.MAX_BLEND - 1, FlixelPipelineKey.blendOf(key));
    assertEquals(FlixelPipelineKey.MAX_FORMAT - 1, FlixelPipelineKey.formatOf(key));
    assertEquals(FlixelPipelineKey.MAX_SHADER - 1, FlixelPipelineKey.shaderOf(key));
  }

  @Test
  void differentInputsGiveDifferentKeys() {
    long base = FlixelPipelineKey.of(1, 2, 3);
    assertNotEquals(base, FlixelPipelineKey.of(2, 2, 3));
    assertNotEquals(base, FlixelPipelineKey.of(1, 3, 3));
    assertNotEquals(base, FlixelPipelineKey.of(1, 2, 4));
    assertEquals(base, FlixelPipelineKey.of(1, 2, 3));
  }

  @Test
  void outOfRangePartsAreRejected() {
    assertThrows(IllegalArgumentException.class, () -> FlixelPipelineKey.of(FlixelPipelineKey.MAX_BLEND, 0, 0));
    assertThrows(IllegalArgumentException.class, () -> FlixelPipelineKey.of(0, FlixelPipelineKey.MAX_FORMAT, 0));
    assertThrows(IllegalArgumentException.class, () -> FlixelPipelineKey.of(0, 0, FlixelPipelineKey.MAX_SHADER));
    assertThrows(IllegalArgumentException.class, () -> FlixelPipelineKey.of(-1, 0, 0));
  }
}
