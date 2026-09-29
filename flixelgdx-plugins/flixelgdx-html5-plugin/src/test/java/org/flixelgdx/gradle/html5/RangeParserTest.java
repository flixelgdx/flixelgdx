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
package org.flixelgdx.gradle.html5;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RangeParserTest {

  @Test
  void absentOrUnsupportedFallsBackToFull() {
    assertNull(RangeParser.parse(null, 100));
    assertNull(RangeParser.parse("items=0-5", 100));
    assertNull(RangeParser.parse("bytes=0-5,10-15", 100));
    assertNull(RangeParser.parse("bytes=abc-def", 100));
  }

  @Test
  void parsesClosedOpenAndSuffixRanges() {
    assertArrayEquals(new long[] { 0, 9 }, RangeParser.parse("bytes=0-9", 100));
    assertArrayEquals(new long[] { 50, 99 }, RangeParser.parse("bytes=50-", 100));
    assertArrayEquals(new long[] { 90, 99 }, RangeParser.parse("bytes=-10", 100));
    assertArrayEquals(new long[] { 0, 99 }, RangeParser.parse("bytes=-500", 100));
  }

  @Test
  void clampsEndToLastByte() {
    assertArrayEquals(new long[] { 10, 99 }, RangeParser.parse("bytes=10-5000", 100));
  }

  @Test
  void unsatisfiableRangesReturnEmpty() {
    assertEquals(0, RangeParser.parse("bytes=100-", 100).length);
    assertEquals(0, RangeParser.parse("bytes=200-300", 100).length);
    assertEquals(0, RangeParser.parse("bytes=-0", 100).length);
    assertEquals(0, RangeParser.parse("bytes=0-", 0).length);
  }
}
