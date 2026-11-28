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

class FlixelColorTest {

  @Test
  void lerpHalfwayBetweenRedAndBlue() {
    FlixelColor a = new FlixelColor(1f, 0f, 0f, 1f);
    FlixelColor b = new FlixelColor(0f, 0f, 1f, 1f);
    a.lerp(b, 0.5f);
    assertEquals(0.5f, a.getRed(), 2e-2f);
    assertEquals(0f, a.getGreen(), 2e-2f);
    assertEquals(0.5f, a.getBlue(), 2e-2f);
  }

  @Test
  void packRoundTrip() {
    FlixelColor c = new FlixelColor(1f, 0.5f, 0.25f, 1f);
    int packed = c.getRgba8888();
    FlixelColor fromPacked = new FlixelColor(packed);
    assertEquals(c.getRed(), fromPacked.getRed(), 2e-2f);
    assertEquals(c.getGreen(), fromPacked.getGreen(), 2e-2f);
    assertEquals(c.getBlue(), fromPacked.getBlue(), 2e-2f);
    assertEquals(c.getAlpha(), fromPacked.getAlpha(), 2e-2f);
  }

  @Test
  void staticFloatBitsMatchInstanceFloatBits() {
    FlixelColor c = new FlixelColor(1f, 0.5f, 0.25f, 0.75f);
    assertEquals(c.toFloatBits(), FlixelColor.toFloatBits(1f, 0.5f, 0.25f, 0.75f));
  }

  @Test
  void channelSettersStoreValuesInRange() {
    FlixelColor c = new FlixelColor(0f, 0f, 0f, 0f);
    c.setRed(0.1f);
    c.setGreen(0.2f);
    c.setBlue(0.3f);
    c.setAlpha(0.4f);
    assertEquals(0.1f, c.getRed(), 1e-6f);
    assertEquals(0.2f, c.getGreen(), 1e-6f);
    assertEquals(0.3f, c.getBlue(), 1e-6f);
    assertEquals(0.4f, c.getAlpha(), 1e-6f);
  }

  @Test
  void channelSettersClampOutOfRangeValues() {
    FlixelColor c = new FlixelColor();
    c.setRed(2f);
    c.setGreen(-1f);
    c.setBlue(1.5f);
    c.setAlpha(-0.5f);
    assertEquals(1f, c.getRed());
    assertEquals(0f, c.getGreen());
    assertEquals(1f, c.getBlue());
    assertEquals(0f, c.getAlpha());
  }

  @Test
  void wholeNumberSettersMapZeroToTwoFiftyFiveOntoZeroToOne() {
    FlixelColor c = new FlixelColor();
    c.setRed(0);
    c.setGreen(255);
    c.setBlue(51);
    assertEquals(0f, c.getRed());
    assertEquals(1f, c.getGreen());
    assertEquals(0.2f, c.getBlue(), 1e-6f);
    assertEquals(1f, c.getAlpha(), "alpha is left alone");
  }

  @Test
  void wholeNumberSettersClampOutOfRangeValues() {
    FlixelColor c = new FlixelColor();
    c.setRed(300);
    c.setGreen(-20);
    c.setBlue(256);
    assertEquals(1f, c.getRed());
    assertEquals(0f, c.getGreen());
    assertEquals(1f, c.getBlue());
  }

  @Test
  void floatConstructorAndSetClampOutOfRangeValues() {
    FlixelColor c = new FlixelColor(-1f, 2f, 0.5f, 3f);
    assertEquals(0f, c.getRed());
    assertEquals(1f, c.getGreen());
    assertEquals(0.5f, c.getBlue());
    assertEquals(1f, c.getAlpha());
    c.set(5f, -5f, 0.25f, -1f);
    assertEquals(1f, c.getRed());
    assertEquals(0f, c.getGreen());
    assertEquals(0.25f, c.getBlue());
    assertEquals(0f, c.getAlpha());
  }

  @Test
  void lerpPastTheTargetIsClamped() {
    FlixelColor c = new FlixelColor(0f, 0.5f, 1f, 1f);
    c.lerp(new FlixelColor(1f, 0.5f, 0f, 1f), 2f);
    assertEquals(1f, c.getRed());
    assertEquals(0.5f, c.getGreen(), 1e-6f);
    assertEquals(0f, c.getBlue());
    assertEquals(1f, c.getAlpha());
  }
}
