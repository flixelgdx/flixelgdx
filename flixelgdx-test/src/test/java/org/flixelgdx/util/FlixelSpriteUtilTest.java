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

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class FlixelSpriteUtilTest {

  @Test
  public void linearGradientPixmap_horizontal_matchesEndpoints() {
    Color start = new Color(1f, 0f, 0f, 1f);
    Color end = new Color(0f, 0f, 1f, 1f);

    Pixmap pm = FlixelSpriteUtil.createLinearGradientPixmap(9, 3, start, end, true);
    try {
      assertColorClose(read(pm, 0, 0), start);
      assertColorClose(read(pm, 8, 2), end);
    } finally {
      pm.dispose();
    }
  }

  @Test
  public void linearGradientPixmap_vertical_matchesEndpoints() {
    Color start = new Color(0f, 1f, 0f, 1f);
    Color end = new Color(1f, 1f, 0f, 1f);

    Pixmap pm = FlixelSpriteUtil.createLinearGradientPixmap(4, 7, start, end, false);
    try {
      assertColorClose(read(pm, 0, 0), start);
      assertColorClose(read(pm, 3, 6), end);
    } finally {
      pm.dispose();
    }
  }

  private static Color read(Pixmap pm, int x, int y) {
    int rgba8888 = pm.getPixel(x, y);
    Color out = new Color();
    Color.rgba8888ToColor(out, rgba8888);
    return out;
  }

  private static void assertColorClose(Color actual, Color expected) {
    float eps = 1f / 255f + 0.0001f;
    assertTrue(Math.abs(actual.r - expected.r) <= eps, "r differs");
    assertTrue(Math.abs(actual.g - expected.g) <= eps, "g differs");
    assertTrue(Math.abs(actual.b - expected.b) <= eps, "b differs");
    assertTrue(Math.abs(actual.a - expected.a) <= eps, "a differs");
  }
}
