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

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelRandomTest {

  @Test
  void sameSeedProducesSameSequence() {
    FlixelRandom a = new FlixelRandom(12345L);
    FlixelRandom b = new FlixelRandom(12345L);
    for (int i = 0; i < 1000; i++) {
      assertEquals(a.nextLong(), b.nextLong());
    }
  }

  @Test
  void differentSeedsDiverge() {
    FlixelRandom a = new FlixelRandom(1L);
    FlixelRandom b = new FlixelRandom(2L);
    boolean anyDifferent = false;
    for (int i = 0; i < 10; i++) {
      if (a.nextLong() != b.nextLong()) {
        anyDifferent = true;
        break;
      }
    }
    assertTrue(anyDifferent);
  }

  @Test
  void setSeedRestartsTheSequence() {
    FlixelRandom rng = new FlixelRandom(99L);
    long first = rng.nextLong();
    rng.nextLong();
    rng.setSeed(99L);
    assertEquals(first, rng.nextLong());
  }

  @Test
  void zeroSeedStillGenerates() {
    FlixelRandom rng = new FlixelRandom(0L);
    // A naive xorshift stuck at zero would return only zeros; ensure it does not.
    boolean sawNonZero = false;
    for (int i = 0; i < 10; i++) {
      if (rng.nextLong() != 0L) {
        sawNonZero = true;
        break;
      }
    }
    assertTrue(sawNonZero);
    assertEquals(0L, rng.getSeed());
  }

  @Test
  void nextIntBoundStaysInRange() {
    FlixelRandom rng = new FlixelRandom(7L);
    for (int i = 0; i < 10000; i++) {
      int value = rng.nextInt(6);
      assertTrue(value >= 0 && value < 6, "out of range: " + value);
    }
  }

  @Test
  void nextIntMinMaxIsInclusive() {
    FlixelRandom rng = new FlixelRandom(7L);
    boolean sawMin = false;
    boolean sawMax = false;
    for (int i = 0; i < 10000; i++) {
      int value = rng.nextInt(1, 6);
      assertTrue(value >= 1 && value <= 6, "out of range: " + value);
      sawMin |= value == 1;
      sawMax |= value == 6;
    }
    assertTrue(sawMin && sawMax);
  }

  @Test
  void nextIntSingleValueRange() {
    FlixelRandom rng = new FlixelRandom(7L);
    assertEquals(5, rng.nextInt(5, 5));
  }

  @Test
  void nextIntRejectsNonPositiveBound() {
    FlixelRandom rng = new FlixelRandom(7L);
    assertThrows(IllegalArgumentException.class, () -> rng.nextInt(0));
    assertThrows(IllegalArgumentException.class, () -> rng.nextInt(3, 2));
  }

  @Test
  void nextFloatStaysInUnitRange() {
    FlixelRandom rng = new FlixelRandom(7L);
    for (int i = 0; i < 10000; i++) {
      float value = rng.nextFloat();
      assertTrue(value >= 0f && value < 1f, "out of range: " + value);
    }
  }

  @Test
  void nextFloatRange() {
    FlixelRandom rng = new FlixelRandom(7L);
    for (int i = 0; i < 10000; i++) {
      float value = rng.nextFloat(10f, 20f);
      assertTrue(value >= 10f && value < 20f, "out of range: " + value);
    }
  }

  @Test
  void nextBoolAlwaysAndNever() {
    FlixelRandom rng = new FlixelRandom(7L);
    for (int i = 0; i < 100; i++) {
      assertTrue(rng.nextBool(1f));
      assertTrue(!rng.nextBool(0f));
    }
  }

  @Test
  void signIsMinusOneOrOne() {
    FlixelRandom rng = new FlixelRandom(7L);
    for (int i = 0; i < 1000; i++) {
      int s = rng.sign();
      assertTrue(s == -1 || s == 1);
    }
  }

  @Test
  void pickReturnsAnElement() {
    FlixelRandom rng = new FlixelRandom(7L);
    Integer[] items = { 10, 20, 30 };
    for (int i = 0; i < 100; i++) {
      Integer chosen = rng.pick(items);
      assertTrue(chosen == 10 || chosen == 20 || chosen == 30);
    }
  }

  @Test
  void pickRejectsEmptyArray() {
    FlixelRandom rng = new FlixelRandom(7L);
    assertThrows(IllegalArgumentException.class, () -> rng.pick(new Integer[0]));
  }

  @Test
  void shufflePreservesElements() {
    FlixelRandom rng = new FlixelRandom(7L);
    Integer[] items = { 1, 2, 3, 4, 5, 6, 7, 8 };
    Integer[] sorted = items.clone();
    rng.shuffle(items);
    Arrays.sort(items);
    assertArrayEquals(sorted, items);
  }
}
