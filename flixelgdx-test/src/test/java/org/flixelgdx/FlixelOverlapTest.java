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
package org.flixelgdx;

import org.flixelgdx.group.FlixelBasicGroup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelOverlapTest {

  private int pairs;

  @AfterEach
  void tearDown() {
    Flixel.setWorldBounds(-10000f, -10000f, 20000f, 20000f);
  }

  @Test
  void gridFindsTheSamePairsAsBruteForce() {
    Random random = new Random(1234L);
    ObjectGroup group1 = randomGroup(random, 300);
    ObjectGroup group2 = randomGroup(random, 300);
    group2.add(new FlixelObject(0f, 500f, 2000f, 16f));

    int expected = 0;
    for (int i = 0; i < group1.getMembers().getSize(); i++) {
      for (int j = 0; j < group2.getMembers().getSize(); j++) {
        if (touches(group1.getMembers().get(i), group2.getMembers().get(j))) {
          expected++;
        }
      }
    }

    assertTrue(Flixel.overlap(group1, group2, (a, b) -> pairs++, null));
    assertTrue(expected > 0);
    assertEquals(expected, pairs);
  }

  @Test
  void selfOverlapTestsEachOrderedPairOnceAndSkipsSelf() {
    Random random = new Random(99L);
    ObjectGroup group = randomGroup(random, 200);
    FlixelObject[] items = group.getMembers().getItems();
    int count = group.getMembers().getSize();

    int expected = 0;
    for (int i = 0; i < count; i++) {
      for (int j = 0; j < count; j++) {
        if (i != j && touches(items[i], items[j])) {
          expected++;
        }
      }
    }

    Flixel.overlap(group, group, (a, b) -> {
      assertFalse(a == b);
      pairs++;
    }, null);
    assertEquals(expected, pairs);
  }

  @Test
  void processCallbackCanRejectPairs() {
    FlixelObject a = new FlixelObject(0f, 0f, 10f, 10f);
    FlixelObject b = new FlixelObject(5f, 5f, 10f, 10f);

    assertFalse(Flixel.overlap(a, b, (x, y) -> pairs++, (x, y) -> false));
    assertEquals(0, pairs);
    assertTrue(Flixel.overlap(a, b, (x, y) -> pairs++, (x, y) -> true));
    assertEquals(1, pairs);
  }

  @Test
  void killedObjectsAreSkippedDuringTheSameCall() {
    FlixelObject player = new FlixelObject(0f, 0f, 10f, 10f);
    ObjectGroup coins = new ObjectGroup();
    coins.add(new FlixelObject(1f, 1f, 4f, 4f));
    coins.add(new FlixelObject(2f, 2f, 4f, 4f));

    Flixel.overlap(player, coins, (p, c) -> {
      pairs++;
      p.kill();
    }, null);
    assertEquals(1, pairs);
  }

  @Test
  void callbacksCanCallOverlapAgain() {
    Random random = new Random(7L);
    ObjectGroup group1 = randomGroup(random, 150);
    ObjectGroup group2 = randomGroup(random, 150);
    FlixelObject a = new FlixelObject(0f, 0f, 10f, 10f);
    FlixelObject b = new FlixelObject(5f, 5f, 10f, 10f);

    int[] nested = new int[1];
    int[] outer = new int[1];
    Flixel.overlap(group1, group2, (x, y) -> {
      outer[0]++;
      if (Flixel.overlap(a, b)) {
        nested[0]++;
      }
    }, null);

    assertTrue(outer[0] > 0);
    assertEquals(outer[0], nested[0]);
  }

  @Test
  void objectsOutsideTheWorldBoundsAreSkipped() {
    FlixelObject a = new FlixelObject(500f, 500f, 10f, 10f);
    FlixelObject b = new FlixelObject(505f, 505f, 10f, 10f);
    assertTrue(Flixel.overlap(a, b));

    Flixel.setWorldBounds(0f, 0f, 100f, 100f);
    assertFalse(Flixel.overlap(a, b));
  }

  @Test
  void collideSeparatesObjectsFoundByTheGrid() {
    ObjectGroup walls = new ObjectGroup();
    ObjectGroup crates = new ObjectGroup();
    for (int i = 0; i < 100; i++) {
      FlixelObject wall = new FlixelObject(i * 40f, 100f, 32f, 32f);
      wall.setImmovable(true);
      walls.add(wall);

      FlixelObject crate = new FlixelObject(i * 40f, 70f, 32f, 32f);
      crate.lastX = crate.getX();
      crate.lastY = crate.getY() - 4f;
      crates.add(crate);
    }

    assertTrue(Flixel.collide(crates, walls));
    FlixelObject[] items = crates.getMembers().getItems();
    for (int i = 0; i < crates.getMembers().getSize(); i++) {
      assertEquals(68f, items[i].getY(), 1e-4f);
    }
  }

  private static ObjectGroup randomGroup(Random random, int count) {
    ObjectGroup group = new ObjectGroup();
    for (int i = 0; i < count; i++) {
      group.add(new FlixelObject(random.nextFloat() * 1000f - 200f, random.nextFloat() * 1000f - 200f,
          4f + random.nextFloat() * 28f, 4f + random.nextFloat() * 28f));
    }
    return group;
  }

  private static boolean touches(FlixelObject a, FlixelObject b) {
    return a.getX() < b.getX() + b.getWidth()
        && a.getX() + a.getWidth() > b.getX()
        && a.getY() < b.getY() + b.getHeight()
        && a.getY() + a.getHeight() > b.getY();
  }

  private static final class ObjectGroup extends FlixelBasicGroup<FlixelObject> {
    ObjectGroup() {
      super(FlixelObject[]::new);
    }
  }
}
