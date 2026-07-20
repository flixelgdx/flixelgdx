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
package org.flixelgdx.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FlixelMathUtilTest {

  private static final float DELTA = 1e-5f;

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
}
