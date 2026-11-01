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
package org.flixelgdx.backend.gpu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FlixelRingAllocatorTest {

  @Test
  void allocationsAreSequentialAndAligned() {
    FlixelRingAllocator ring = new FlixelRingAllocator(1024);
    assertEquals(0, ring.allocate(80, 256));
    assertEquals(256, ring.allocate(80, 256));
    assertEquals(336, ring.allocate(4, 4));
    assertEquals(340, ring.allocate(8, 4));
    assertEquals(348, ring.getUsed());
  }

  @Test
  void fullRingsReturnMinusOneAndDoNotAdvance() {
    FlixelRingAllocator ring = new FlixelRingAllocator(100);
    assertEquals(0, ring.allocate(60, 4));
    assertEquals(-1, ring.allocate(41, 4));
    assertEquals(60, ring.getUsed());
    assertEquals(60, ring.allocate(40, 4));
    assertEquals(-1, ring.allocate(1, 4));
  }

  @Test
  void anAllocationThatOnlyFitsWithoutPaddingFailsBecauseOfThePadding() {
    FlixelRingAllocator ring = new FlixelRingAllocator(300);
    assertEquals(0, ring.allocate(10, 4));
    assertEquals(-1, ring.allocate(100, 256));
  }

  @Test
  void resetRewindsButKeepsThePeak() {
    FlixelRingAllocator ring = new FlixelRingAllocator(1000);
    ring.allocate(700, 4);
    ring.reset();
    assertEquals(0, ring.getUsed());
    assertEquals(700, ring.getPeak());
    assertEquals(0, ring.allocate(10, 4));
    ring.resetPeak();
    assertEquals(10, ring.getPeak());
  }

  @Test
  void aWholeRingCanBeFilledExactly() {
    FlixelRingAllocator ring = new FlixelRingAllocator(512);
    assertEquals(0, ring.allocate(256, 256));
    assertEquals(256, ring.allocate(256, 256));
    assertEquals(-1, ring.allocate(1, 1));
    assertEquals(512, ring.getCapacity());
  }
}
