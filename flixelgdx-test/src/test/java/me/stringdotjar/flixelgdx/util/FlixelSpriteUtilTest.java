/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.util;

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

