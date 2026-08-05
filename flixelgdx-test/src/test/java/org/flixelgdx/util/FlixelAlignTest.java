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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelAlignTest {

  @Test
  void cornersCombineEdgeBits() {
    assertEquals(FlixelAlign.TOP | FlixelAlign.LEFT, FlixelAlign.TOP_LEFT);
    assertEquals(FlixelAlign.TOP | FlixelAlign.RIGHT, FlixelAlign.TOP_RIGHT);
    assertEquals(FlixelAlign.BOTTOM | FlixelAlign.LEFT, FlixelAlign.BOTTOM_LEFT);
    assertEquals(FlixelAlign.BOTTOM | FlixelAlign.RIGHT, FlixelAlign.BOTTOM_RIGHT);
  }

  @Test
  void horizontalPredicates() {
    assertTrue(FlixelAlign.isLeft(FlixelAlign.TOP_LEFT));
    assertFalse(FlixelAlign.isRight(FlixelAlign.TOP_LEFT));
    assertTrue(FlixelAlign.isRight(FlixelAlign.BOTTOM_RIGHT));
    assertFalse(FlixelAlign.isLeft(FlixelAlign.BOTTOM_RIGHT));
  }

  @Test
  void verticalPredicates() {
    assertTrue(FlixelAlign.isTop(FlixelAlign.TOP_RIGHT));
    assertFalse(FlixelAlign.isBottom(FlixelAlign.TOP_RIGHT));
    assertTrue(FlixelAlign.isBottom(FlixelAlign.BOTTOM_LEFT));
    assertFalse(FlixelAlign.isTop(FlixelAlign.BOTTOM_LEFT));
  }

  @Test
  void centerPredicates() {
    assertTrue(FlixelAlign.isCenterHorizontal(FlixelAlign.TOP));
    assertFalse(FlixelAlign.isCenterHorizontal(FlixelAlign.TOP_LEFT));
    assertTrue(FlixelAlign.isCenterVertical(FlixelAlign.LEFT));
    assertFalse(FlixelAlign.isCenterVertical(FlixelAlign.TOP_LEFT));
  }

  @Test
  void pureCenterIsCenteredOnBothAxes() {
    assertTrue(FlixelAlign.isCenterHorizontal(FlixelAlign.CENTER));
    assertTrue(FlixelAlign.isCenterVertical(FlixelAlign.CENTER));
  }
}
