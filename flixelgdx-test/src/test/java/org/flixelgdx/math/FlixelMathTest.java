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

class FlixelMathTest {

  private static final float DELTA = 1e-5f;

  // A looser tolerance for the lookup-table trig, which trades accuracy for speed.
  private static final float TRIG_DELTA = 1e-3f;

  @Test
  void roundTwoDecimals() {
    assertEquals(3.15f, FlixelMath.round(3.145f, 2), DELTA);
  }

  @Test
  void roundZeroPlaces() {
    assertEquals(4f, FlixelMath.round(3.7f, 0), DELTA);
  }

  @Test
  void roundZeroValue() {
    assertEquals(0f, FlixelMath.round(0f, 2), DELTA);
  }

  @Test
  void roundNegativeValue() {
    // -1.6 * 10 = -16.0, Math.round(-16.0f) = -16, so result is -1.6 exactly.
    assertEquals(-1.6f, FlixelMath.round(-1.6f, 1), DELTA);
  }

  @Test
  void roundHalfUp() {
    assertEquals(1f, FlixelMath.round(0.5f, 0), DELTA);
  }

  @Test
  void roundLargeWholeNumber() {
    assertEquals(1000f, FlixelMath.round(999.9f, 0), DELTA);
  }

  @Test
  void roundOneDecimalPlace() {
    assertEquals(1.2f, FlixelMath.round(1.23f, 1), DELTA);
  }

  @Test
  void sinMatchesMathAtCardinalAngles() {
    assertEquals(0f, FlixelMath.sin(0f), TRIG_DELTA);
    assertEquals(1f, FlixelMath.sin(FlixelMath.HALF_PI), TRIG_DELTA);
    assertEquals(0f, FlixelMath.sin(FlixelMath.PI), TRIG_DELTA);
    assertEquals(-1f, FlixelMath.sin(FlixelMath.PI + FlixelMath.HALF_PI), TRIG_DELTA);
  }

  @Test
  void cosMatchesMathAtCardinalAngles() {
    assertEquals(1f, FlixelMath.cos(0f), TRIG_DELTA);
    assertEquals(0f, FlixelMath.cos(FlixelMath.HALF_PI), TRIG_DELTA);
    assertEquals(-1f, FlixelMath.cos(FlixelMath.PI), TRIG_DELTA);
  }

  @Test
  void sinTracksMathSinAcrossTheCircle() {
    for (float a = -FlixelMath.PI2; a <= FlixelMath.PI2; a += 0.05f) {
      assertEquals((float) Math.sin(a), FlixelMath.sin(a), TRIG_DELTA);
    }
  }

  @Test
  void clampInt() {
    assertEquals(5, FlixelMath.clamp(5, 0, 10));
    assertEquals(0, FlixelMath.clamp(-3, 0, 10));
    assertEquals(10, FlixelMath.clamp(42, 0, 10));
  }

  @Test
  void clampFloat() {
    assertEquals(5f, FlixelMath.clamp(5f, 0f, 10f), DELTA);
    assertEquals(0f, FlixelMath.clamp(-3f, 0f, 10f), DELTA);
    assertEquals(10f, FlixelMath.clamp(42f, 0f, 10f), DELTA);
  }

  @Test
  void lerpEndpointsAndMidpoint() {
    assertEquals(0f, FlixelMath.lerp(0f, 10f, 0f), DELTA);
    assertEquals(10f, FlixelMath.lerp(0f, 10f, 1f), DELTA);
    assertEquals(5f, FlixelMath.lerp(0f, 10f, 0.5f), DELTA);
  }

  @Test
  void lerpAngleTakesShortestPath() {
    // 350 -> 10 should sweep +20 forward (to 370, equivalent to 10), not backward.
    assertEquals(370f, FlixelMath.lerpAngle(350f, 10f, 1f), DELTA);
    assertEquals(355f, FlixelMath.lerpAngle(350f, 10f, 0.25f), DELTA);
  }

  @Test
  void approachNeverOvershoots() {
    assertEquals(10f, FlixelMath.approach(8f, 10f, 5f), DELTA);
    assertEquals(0f, FlixelMath.approach(2f, 0f, 5f), DELTA);
    assertEquals(9f, FlixelMath.approach(8f, 20f, 1f), DELTA);
  }

  @Test
  void wrapCyclesInsteadOfClamping() {
    assertEquals(-170f, FlixelMath.wrap(190f, -180f, 180f), DELTA);
    assertEquals(170f, FlixelMath.wrap(-190f, -180f, 180f), DELTA);
    assertEquals(5f, FlixelMath.wrap(5f, 0f, 10f), DELTA);
  }

  @Test
  void remapPreservesRelativePosition() {
    assertEquals(50f, FlixelMath.remap(5f, 0f, 10f, 0f, 100f), DELTA);
    assertEquals(0f, FlixelMath.remap(0f, 0f, 10f, 0f, 100f), DELTA);
  }

  @Test
  void snapToGrid() {
    assertEquals(10f, FlixelMath.snap(12f, 5f), DELTA);
    assertEquals(15f, FlixelMath.snap(13f, 5f), DELTA);
    assertEquals(7f, FlixelMath.snap(7f, 0f), DELTA);
  }

  @Test
  void signOfReportsMinusZeroPlus() {
    assertEquals(-1, FlixelMath.signOf(-4f));
    assertEquals(0, FlixelMath.signOf(0f));
    assertEquals(1, FlixelMath.signOf(4f));
  }

  @Test
  void isEqualWithinEpsilon() {
    assertTrue(FlixelMath.isEqual(1.0f, 1.0000001f, 1e-5f));
    assertFalse(FlixelMath.isEqual(1.0f, 1.1f, 1e-5f));
  }

  @Test
  void isEqualWithDefaultTolerance() {
    assertTrue(FlixelMath.isEqual(1.0f, 1.0f));
    assertFalse(FlixelMath.isEqual(1.0f, 1.001f));
  }

  @Test
  void sinDegAndCosDegMatchDegrees() {
    assertEquals(0f, FlixelMath.sinDeg(0f), TRIG_DELTA);
    assertEquals(1f, FlixelMath.sinDeg(90f), TRIG_DELTA);
    assertEquals(1f, FlixelMath.cosDeg(0f), TRIG_DELTA);
    assertEquals(0f, FlixelMath.cosDeg(90f), TRIG_DELTA);
  }

  @Test
  void atan2MatchesMath() {
    assertEquals(0f, FlixelMath.atan2(0f, 1f), DELTA);
    assertEquals((float) Math.atan2(1, 1), FlixelMath.atan2(1f, 1f), DELTA);
  }
}
