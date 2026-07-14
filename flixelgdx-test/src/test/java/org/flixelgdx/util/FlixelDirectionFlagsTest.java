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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelDirectionFlagsTest {

  @Test
  void primitiveConstants() {
    assertEquals(0x0000, FlixelDirectionFlags.NONE);
    assertEquals(0x0001, FlixelDirectionFlags.LEFT);
    assertEquals(0x0010, FlixelDirectionFlags.RIGHT);
    assertEquals(0x0100, FlixelDirectionFlags.UP);
    assertEquals(0x1000, FlixelDirectionFlags.DOWN);
  }

  @Test
  void anyEqualsUnionOfAllDirections() {
    assertEquals(
        FlixelDirectionFlags.LEFT | FlixelDirectionFlags.RIGHT | FlixelDirectionFlags.UP | FlixelDirectionFlags.DOWN,
        FlixelDirectionFlags.ANY);
  }

  @Test
  void collisionAliasesMatchDirections() {
    assertEquals(FlixelDirectionFlags.DOWN, FlixelDirectionFlags.FLOOR);
    assertEquals(FlixelDirectionFlags.UP, FlixelDirectionFlags.CEILING);
    assertEquals(FlixelDirectionFlags.LEFT | FlixelDirectionFlags.RIGHT, FlixelDirectionFlags.WALL);
  }

  @Test
  void flagsDoNotOverlap() {
    assertEquals(0, FlixelDirectionFlags.LEFT & FlixelDirectionFlags.RIGHT);
    assertEquals(0, FlixelDirectionFlags.LEFT & FlixelDirectionFlags.UP);
    assertEquals(0, FlixelDirectionFlags.LEFT & FlixelDirectionFlags.DOWN);
    assertEquals(0, FlixelDirectionFlags.RIGHT & FlixelDirectionFlags.UP);
    assertEquals(0, FlixelDirectionFlags.RIGHT & FlixelDirectionFlags.DOWN);
    assertEquals(0, FlixelDirectionFlags.UP & FlixelDirectionFlags.DOWN);
  }

  @Test
  void maskingIsolateSingleFlag() {
    int combined = FlixelDirectionFlags.LEFT | FlixelDirectionFlags.UP;
    assertNotEquals(0, combined & FlixelDirectionFlags.LEFT);
    assertNotEquals(0, combined & FlixelDirectionFlags.UP);
    assertEquals(0, combined & FlixelDirectionFlags.RIGHT);
    assertEquals(0, combined & FlixelDirectionFlags.DOWN);
  }

  @Test
  void anyContainsAllPrimitiveFlags() {
    assertTrue((FlixelDirectionFlags.ANY & FlixelDirectionFlags.LEFT) != 0);
    assertTrue((FlixelDirectionFlags.ANY & FlixelDirectionFlags.RIGHT) != 0);
    assertTrue((FlixelDirectionFlags.ANY & FlixelDirectionFlags.UP) != 0);
    assertTrue((FlixelDirectionFlags.ANY & FlixelDirectionFlags.DOWN) != 0);
  }

  @Test
  void noneDoesNotMatchAnyFlag() {
    assertEquals(0, FlixelDirectionFlags.NONE & FlixelDirectionFlags.LEFT);
    assertEquals(0, FlixelDirectionFlags.NONE & FlixelDirectionFlags.RIGHT);
    assertEquals(0, FlixelDirectionFlags.NONE & FlixelDirectionFlags.UP);
    assertEquals(0, FlixelDirectionFlags.NONE & FlixelDirectionFlags.DOWN);
    assertEquals(0, FlixelDirectionFlags.NONE & FlixelDirectionFlags.ANY);
  }
}
