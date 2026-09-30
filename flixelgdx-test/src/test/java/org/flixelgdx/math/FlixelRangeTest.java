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

import org.flixelgdx.FlixelHeadlessExtension;
import org.flixelgdx.util.FlixelColor;
import org.flixelgdx.util.FlixelColorRange;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(FlixelHeadlessExtension.class)
class FlixelRangeTest {

  @Test
  void rangeLerpsAndIsActiveOnlyWhenEndsDiffer() {
    FlixelRange range = new FlixelRange(1f, 0f);
    assertTrue(range.active);
    assertEquals(0.25f, range.lerp(0.75f), 1e-6f);
    range.set(3f, 3f);
    assertFalse(range.active);
  }

  @Test
  void pointRangeLerpsEachAxis() {
    FlixelPointRange range = new FlixelPointRange(0f, 10f, 10f, 30f);
    assertTrue(range.active);
    assertEquals(5f, range.lerpX(0.5f), 1e-6f);
    assertEquals(20f, range.lerpY(0.5f), 1e-6f);
  }

  @Test
  void colorRangeWritesIntoGivenColor() {
    FlixelColorRange range = new FlixelColorRange().set(FlixelColor.BLACK, FlixelColor.WHITE);
    FlixelColor out = new FlixelColor();
    assertSame(out, range.lerp(0.5f, out));
    assertEquals(0.5f, out.r, 1e-6f);
    assertEquals(0.5f, out.g, 1e-6f);
    assertEquals(0.5f, out.b, 1e-6f);
    assertEquals(1f, out.a, 1e-6f);
  }

  @Test
  void colorRangeKeepsOwnedCopies() {
    FlixelColor source = new FlixelColor(FlixelColor.RED);
    FlixelColorRange range = new FlixelColorRange().set(source, source);
    source.set(FlixelColor.BLUE);
    assertEquals(1f, range.start.r, 1e-6f);
    assertFalse(range.active);
  }

  @Test
  void rangeBoundsSetCopiesStartToEnd() {
    FlixelRangeBounds range = new FlixelRangeBounds(0f);
    range.set(2f, 4f);
    assertEquals(2f, range.end.min);
    assertEquals(4f, range.end.max);
    assertFalse(range.changes());
    range.set(2f, 4f, 0f, 0f);
    assertTrue(range.changes());
  }
}
