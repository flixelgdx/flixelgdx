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

import io.github.stringdotjar.jwebgpu.GpuTextureFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelGpuBlockFormatTest {

  @Test
  void blockMathRoundsPartialBlocksUp() {
    FlixelGpuBlockFormat astc = FlixelGpuBlockFormat.ASTC_4X4;
    assertEquals(13, astc.blocks(50));
    assertEquals(52, astc.padded(50));
    assertEquals(1, astc.blocks(0), "a level is never smaller than one pixel");
    assertEquals(13 * 10 * 16, astc.levelBytes(50, 37));
    assertEquals(13 * 10 * 8, FlixelGpuBlockFormat.ETC2_RGB8.levelBytes(50, 37));
    assertEquals(50 * 37 * 4, FlixelGpuBlockFormat.RGBA8.levelBytes(50, 37));
    assertEquals(50, FlixelGpuBlockFormat.RGBA8.padded(50));
  }

  @Test
  void vulkanFormatsMapToUnormFormats() {
    assertEquals(FlixelGpuBlockFormat.ASTC_4X4, FlixelGpuBlockFormat.fromVkFormat(157));
    assertEquals(FlixelGpuBlockFormat.ASTC_4X4, FlixelGpuBlockFormat.fromVkFormat(158));
    assertEquals(FlixelGpuBlockFormat.ETC2_RGB8, FlixelGpuBlockFormat.fromVkFormat(148));
    assertEquals(FlixelGpuBlockFormat.BC7, FlixelGpuBlockFormat.fromVkFormat(146));
    assertEquals(FlixelGpuBlockFormat.RGBA8, FlixelGpuBlockFormat.fromVkFormat(43));
    assertNull(FlixelGpuBlockFormat.fromVkFormat(0));
    assertEquals(GpuTextureFormat.ASTC_4X4_UNORM, FlixelGpuBlockFormat.ASTC_4X4.getGpuFormat());
    assertEquals(GpuTextureFormat.BC7_RGBA_UNORM, FlixelGpuBlockFormat.BC7.getGpuFormat());
  }

  @Test
  void onlyRgba8NeedsNoFeature() {
    for (FlixelGpuBlockFormat format : FlixelGpuBlockFormat.values()) {
      assertEquals(format != FlixelGpuBlockFormat.RGBA8, format.isCompressed());
      assertEquals(format == FlixelGpuBlockFormat.RGBA8, format.getFeature() == null);
    }
    assertFalse(FlixelGpuBlockFormat.RGBA8.isCompressed());
    assertTrue((FlixelGpuBlockFormat.BC7.bit() & FlixelGpuBlockFormat.BC3.bit()) == 0);
  }
}
