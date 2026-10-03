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
package org.flixelgdx.text;

import org.flixelgdx.util.FlixelColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/** Verifies that {@link FlixelText} borders fade together with the text's alpha. */
class FlixelTextBorderAlphaTest {

  private static final float EPSILON = 0.0001f;

  @Test
  void borderColorIsMultipliedByTextAlpha() {
    FlixelText text = new FlixelText(0f, 0f, 0f, "hi");
    text.setBorderStyle(FlixelText.BorderStyle.OUTLINE, new FlixelColor(1f, 0.5f, 0.25f, 0.8f));
    text.setAlpha(0.5f);

    FlixelColor resolved = text.resolveBorderColor();

    assertEquals(1f, resolved.r, EPSILON);
    assertEquals(0.5f, resolved.g, EPSILON);
    assertEquals(0.25f, resolved.b, EPSILON);
    assertEquals(0.4f, resolved.a, EPSILON);
  }

  @Test
  void borderColorFieldIsNotMutated() {
    FlixelText text = new FlixelText(0f, 0f, 0f, "hi");
    text.setBorderStyle(FlixelText.BorderStyle.SHADOW, new FlixelColor(0f, 0f, 0f, 1f));
    text.setAlpha(0.25f);

    FlixelColor first = text.resolveBorderColor();
    FlixelColor second = text.resolveBorderColor();

    assertEquals(1f, text.getBorderColor().a, EPSILON);
    assertEquals(0.25f, second.a, EPSILON);
    assertSame(first, second);
  }
}
