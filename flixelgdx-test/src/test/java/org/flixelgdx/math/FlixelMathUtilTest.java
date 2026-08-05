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
package org.flixelgdx.math;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelMathUtilTest {

  private static final float DELTA = 1e-5f;

  // A looser tolerance for the lookup-table trig, which trades accuracy for speed.
  private static final float TRIG_DELTA = 1e-3f;

  @Test
  void roundTwoDecimals() {
    assertEquals(3.15f, FlixelMathUtil.round(3.145f, 2), DELTA);
  }

  @Test
  void roundZeroPlaces() {
    assertEquals(4f, FlixelMathUtil.round(3.7f, 0), DELTA);
  }

  @Test
  void roundZeroValue() {
    assertEquals(0f, FlixelMathUtil.round(0f, 2), DELTA);
  }

  @Test
  void roundNegativeValue() {
    // -1.6 * 10 = -16.0, Math.round(-16.0f) = -16, so result is -1.6 exactly.
    assertEquals(-1.6f, FlixelMathUtil.round(-1.6f, 1), DELTA);
  }

  @Test
  void roundHalfUp() {
    assertEquals(1f, FlixelMathUtil.round(0.5f, 0), DELTA);
  }

  @Test
  void roundLargeWholeNumber() {
    assertEquals(1000f, FlixelMathUtil.round(999.9f, 0), DELTA);
  }

  @Test
  void roundOneDecimalPlace() {
    assertEquals(1.2f, FlixelMathUtil.round(1.23f, 1), DELTA);
  }

  @Test
  void sinMatchesMathAtCardinalAngles() {
    assertEquals(0f, FlixelMathUtil.sin(0f), TRIG_DELTA);
    assertEquals(1f, FlixelMathUtil.sin(FlixelMathUtil.HALF_PI), TRIG_DELTA);
    assertEquals(0f, FlixelMathUtil.sin(FlixelMathUtil.PI), TRIG_DELTA);
    assertEquals(-1f, FlixelMathUtil.sin(FlixelMathUtil.PI + FlixelMathUtil.HALF_PI), TRIG_DELTA);
  }

  @Test
  void cosMatchesMathAtCardinalAngles() {
    assertEquals(1f, FlixelMathUtil.cos(0f), TRIG_DELTA);
    assertEquals(0f, FlixelMathUtil.cos(FlixelMathUtil.HALF_PI), TRIG_DELTA);
    assertEquals(-1f, FlixelMathUtil.cos(FlixelMathUtil.PI), TRIG_DELTA);
  }

  @Test
  void sinTracksMathSinAcrossTheCircle() {
    for (float a = -FlixelMathUtil.PI2; a <= FlixelMathUtil.PI2; a += 0.05f) {
      assertEquals((float) Math.sin(a), FlixelMathUtil.sin(a), TRIG_DELTA);
    }
  }

  @Test
  void clampInt() {
    assertEquals(5, FlixelMathUtil.clamp(5, 0, 10));
    assertEquals(0, FlixelMathUtil.clamp(-3, 0, 10));
    assertEquals(10, FlixelMathUtil.clamp(42, 0, 10));
  }

  @Test
  void clampFloat() {
    assertEquals(5f, FlixelMathUtil.clamp(5f, 0f, 10f), DELTA);
    assertEquals(0f, FlixelMathUtil.clamp(-3f, 0f, 10f), DELTA);
    assertEquals(10f, FlixelMathUtil.clamp(42f, 0f, 10f), DELTA);
  }

  @Test
  void lerpEndpointsAndMidpoint() {
    assertEquals(0f, FlixelMathUtil.lerp(0f, 10f, 0f), DELTA);
    assertEquals(10f, FlixelMathUtil.lerp(0f, 10f, 1f), DELTA);
    assertEquals(5f, FlixelMathUtil.lerp(0f, 10f, 0.5f), DELTA);
  }

  @Test
  void lerpAngleTakesShortestPath() {
    // 350 -> 10 should sweep +20 forward (to 370, equivalent to 10), not backward.
    assertEquals(370f, FlixelMathUtil.lerpAngle(350f, 10f, 1f), DELTA);
    assertEquals(355f, FlixelMathUtil.lerpAngle(350f, 10f, 0.25f), DELTA);
  }

  @Test
  void approachNeverOvershoots() {
    assertEquals(10f, FlixelMathUtil.approach(8f, 10f, 5f), DELTA);
    assertEquals(0f, FlixelMathUtil.approach(2f, 0f, 5f), DELTA);
    assertEquals(9f, FlixelMathUtil.approach(8f, 20f, 1f), DELTA);
  }

  @Test
  void wrapCyclesInsteadOfClamping() {
    assertEquals(-170f, FlixelMathUtil.wrap(190f, -180f, 180f), DELTA);
    assertEquals(170f, FlixelMathUtil.wrap(-190f, -180f, 180f), DELTA);
    assertEquals(5f, FlixelMathUtil.wrap(5f, 0f, 10f), DELTA);
  }

  @Test
  void remapPreservesRelativePosition() {
    assertEquals(50f, FlixelMathUtil.remap(5f, 0f, 10f, 0f, 100f), DELTA);
    assertEquals(0f, FlixelMathUtil.remap(0f, 0f, 10f, 0f, 100f), DELTA);
  }

  @Test
  void snapToGrid() {
    assertEquals(10f, FlixelMathUtil.snap(12f, 5f), DELTA);
    assertEquals(15f, FlixelMathUtil.snap(13f, 5f), DELTA);
    assertEquals(7f, FlixelMathUtil.snap(7f, 0f), DELTA);
  }

  @Test
  void signOfReportsMinusZeroPlus() {
    assertEquals(-1, FlixelMathUtil.signOf(-4f));
    assertEquals(0, FlixelMathUtil.signOf(0f));
    assertEquals(1, FlixelMathUtil.signOf(4f));
  }

  @Test
  void isEqualWithinEpsilon() {
    assertTrue(FlixelMathUtil.isEqual(1.0f, 1.0000001f, 1e-5f));
    assertFalse(FlixelMathUtil.isEqual(1.0f, 1.1f, 1e-5f));
  }
}
