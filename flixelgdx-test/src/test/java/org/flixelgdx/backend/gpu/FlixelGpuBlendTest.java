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

import org.flixelgdx.util.FlixelBlendMode;
import org.junit.jupiter.api.Test;

import io.github.stringdotjar.jwebgpu.GpuBlendFactor;
import io.github.stringdotjar.jwebgpu.GpuBlendOperation;
import io.github.stringdotjar.jwebgpu.GpuBlendState;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelGpuBlendTest {

  @Test
  void everyBlendModeHasAUniqueIdBelowTheKeyLimit() {
    boolean[] seen = new boolean[FlixelPipelineKey.MAX_BLEND];
    for (FlixelBlendMode mode : FlixelBlendMode.values()) {
      int id = FlixelGpuBlend.id(mode);
      assertTrue(id >= 0 && id < FlixelPipelineKey.MAX_BLEND);
      assertFalse(seen[id], "duplicate id for " + mode);
      seen[id] = true;
    }
    assertFalse(seen[FlixelGpuBlend.FORCE_ALPHA], "the internal id must stay free");
  }

  @Test
  void noneTurnsBlendingOff() {
    assertFalse(FlixelGpuBlend.fill(FlixelGpuBlend.NONE, new GpuBlendState()));
  }

  @Test
  void normalIsPremultipliedOver() {
    GpuBlendState s = new GpuBlendState();
    assertTrue(FlixelGpuBlend.fill(FlixelGpuBlend.NORMAL, s));
    assertEquals(GpuBlendFactor.ONE, s.color.srcFactor);
    assertEquals(GpuBlendFactor.ONE_MINUS_SRC_ALPHA, s.color.dstFactor);
    assertEquals(GpuBlendOperation.ADD, s.color.operation);
    assertEquals(GpuBlendFactor.ONE, s.alpha.srcFactor);
    assertEquals(GpuBlendFactor.ONE_MINUS_SRC_ALPHA, s.alpha.dstFactor);
  }

  @Test
  void straightOutputKeepsTheStraightAlphaFactors() {
    GpuBlendState s = new GpuBlendState();
    assertTrue(FlixelGpuBlend.fill(FlixelGpuBlend.NORMAL, false, s));
    assertEquals(GpuBlendFactor.SRC_ALPHA, s.color.srcFactor);
    assertEquals(GpuBlendFactor.ONE_MINUS_SRC_ALPHA, s.color.dstFactor);
    FlixelGpuBlend.fill(FlixelGpuBlend.ADD, false, s);
    assertEquals(GpuBlendFactor.SRC_ALPHA, s.color.srcFactor);
    assertEquals(GpuBlendFactor.ONE, s.color.dstFactor);
    assertFalse(FlixelGpuBlend.fill(FlixelGpuBlend.NONE, false, s));
  }

  @Test
  void theAlphaChannelAlwaysAccumulatesOverForEveryRealMode() {
    GpuBlendState s = new GpuBlendState();
    for (boolean premultiplied : new boolean[] { true, false }) {
      for (int id = FlixelGpuBlend.NORMAL; id <= FlixelGpuBlend.DARKEN; id++) {
        assertTrue(FlixelGpuBlend.fill(id, premultiplied, s));
        assertEquals(GpuBlendFactor.ONE, s.alpha.srcFactor, "mode " + id);
        assertEquals(GpuBlendFactor.ONE_MINUS_SRC_ALPHA, s.alpha.dstFactor, "mode " + id);
        assertEquals(GpuBlendOperation.ADD, s.alpha.operation, "mode " + id);
      }
    }
  }

  @Test
  void minAndMaxUseOneOneAsWebGpuRequires() {
    GpuBlendState s = new GpuBlendState();
    FlixelGpuBlend.fill(FlixelGpuBlend.LIGHTEN, s);
    assertEquals(GpuBlendOperation.MAX, s.color.operation);
    assertEquals(GpuBlendFactor.ONE, s.color.srcFactor);
    assertEquals(GpuBlendFactor.ONE, s.color.dstFactor);
    FlixelGpuBlend.fill(FlixelGpuBlend.DARKEN, s);
    assertEquals(GpuBlendOperation.MIN, s.color.operation);
    assertEquals(GpuBlendFactor.ONE, s.color.srcFactor);
    assertEquals(GpuBlendFactor.ONE, s.color.dstFactor);
  }

  @Test
  void additiveSubtractiveMultiplyAndScreenMatchTheirPremultipliedFormulas() {
    GpuBlendState s = new GpuBlendState();
    FlixelGpuBlend.fill(FlixelGpuBlend.ADD, s);
    assertEquals(GpuBlendFactor.ONE, s.color.srcFactor);
    assertEquals(GpuBlendFactor.ONE, s.color.dstFactor);
    FlixelGpuBlend.fill(FlixelGpuBlend.SUBTRACT, s);
    assertEquals(GpuBlendOperation.REVERSE_SUBTRACT, s.color.operation);
    assertEquals(GpuBlendFactor.ONE, s.color.srcFactor);
    FlixelGpuBlend.fill(FlixelGpuBlend.MULTIPLY, s);
    assertEquals(GpuBlendFactor.DST, s.color.srcFactor);
    assertEquals(GpuBlendFactor.ONE_MINUS_SRC_ALPHA, s.color.dstFactor);
    FlixelGpuBlend.fill(FlixelGpuBlend.SCREEN, s);
    assertEquals(GpuBlendFactor.ONE, s.color.srcFactor);
    assertEquals(GpuBlendFactor.ONE_MINUS_SRC, s.color.dstFactor);
  }

  @Test
  void forceAlphaKeepsTheDestinationColorAndWritesTheSourceAlpha() {
    GpuBlendState s = new GpuBlendState();
    FlixelGpuBlend.fill(FlixelGpuBlend.FORCE_ALPHA, s);
    assertEquals(GpuBlendFactor.ZERO, s.color.srcFactor);
    assertEquals(GpuBlendFactor.ONE, s.color.dstFactor);
    assertEquals(GpuBlendFactor.ONE, s.alpha.srcFactor);
    assertEquals(GpuBlendFactor.ZERO, s.alpha.dstFactor);
  }
}
