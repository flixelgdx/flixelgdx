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
package me.stringdotjar.flixelgdx.util;

import me.stringdotjar.flixelgdx.GdxHeadlessExtension;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(GdxHeadlessExtension.class)
class FlixelColorTest {

  @Test
  void lerpHalfwayBetweenRedAndBlue() {
    FlixelColor a = new FlixelColor(1f, 0f, 0f, 1f);
    FlixelColor b = new FlixelColor(0f, 0f, 1f, 1f);
    a.getGdxColor().lerp(b.getGdxColor(), 0.5f);
    assertEquals(0.5f, a.getGdxColor().r, 2e-2f);
    assertEquals(0f, a.getGdxColor().g, 2e-2f);
    assertEquals(0.5f, a.getGdxColor().b, 2e-2f);
  }

  @Test
  void packRoundTrip() {
    FlixelColor c = new FlixelColor(1f, 0.5f, 0.25f, 1f);
    int packed = c.getColor();
    FlixelColor fromPacked = new FlixelColor(packed);
    assertEquals(c.getGdxColor().r, fromPacked.getGdxColor().r, 2e-2f);
    assertEquals(c.getGdxColor().g, fromPacked.getGdxColor().g, 2e-2f);
    assertEquals(c.getGdxColor().b, fromPacked.getGdxColor().b, 2e-2f);
    assertEquals(c.getGdxColor().a, fromPacked.getGdxColor().a, 2e-2f);
  }
}
