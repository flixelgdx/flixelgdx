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
package org.flixelgdx.physics;

import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelBasic;
import org.flixelgdx.FlixelObject;
import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.group.FlixelGroupable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

/**
 * The broad phase behind {@link Flixel#overlap} and {@link Flixel#collide}.
 *
 * <p>Game code normally never touches this class: call {@code Flixel.overlap(...)} or
 * {@code Flixel.collide(...)} instead, which handle {@code null} arguments (meaning the current state) and pass the
 * right callbacks. Calling {@link #overlap(FlixelBasic, FlixelBasic, BiConsumer, BiPredicate)} directly is fine for
 * power users, but it skips that convenience.
 *
 * <p>Checking every object on one side against every object on the other side gets slow fast: two groups of 200 are
 * 40,000 checks. This class first flattens both sides into plain lists of {@link FlixelObject}s, then decides how to
 * find the pairs worth testing:
 *
 * <ul>
 *   <li>When there are only a few possible pairs (for example one player against a group of coins), it simply tests
 *       every pair. Building a grid would cost more than it saves.</li>
 *   <li>Otherwise it drops every object on the second side into a uniform grid of square cells (a spatial hash, so the
 *       grid needs no fixed size) and only tests each object on the first side against the objects that share a cell
 *       with it.</li>
 * </ul>
 *
 * <p>Think of it like sorting mail by street before delivering it: instead of walking past every house for every
 * letter, each letter only visits the houses on its own street.
 *
 * <p>Every buffer is kept and reused between calls, so a call allocates nothing once the buffers have grown to fit the
 * game's largest groups. Objects whose bounds cover too many cells (such as a long floor) are kept in a separate list
 * that every query checks, so they never flood the grid.
 */
public final class FlixelCollisionGrid {

  /** The largest number of cells one object may cover before it is tested against everything instead. */
  public static final int MAX_CELLS_PER_OBJECT = 16;

  /**
   * How expensive a grid insert or query is compared to one plain pair test. The grid is only used when
   * {@code count1 * count2 > GRID_COST * (count1 + count2)}.
   */
  public static final int GRID_COST = 16;

  /** One grid per nesting level, so a callback can safely call {@link Flixel#overlap} again. */
  @SuppressWarnings("checkstyle:ConstantName")
  private static final FlixelArray<FlixelCollisionGrid> contexts = new FlixelArray<>(FlixelCollisionGrid[]::new, 2);
  private static int depth;

  private final FlixelArray<FlixelObject> list1 = new FlixelArray<>(FlixelObject[]::new, 64);
  private final FlixelArray<FlixelObject> list2 = new FlixelArray<>(FlixelObject[]::new, 64);

  /** The first entry in each bucket, or {@code -1} when the bucket is empty. */
  private int[] heads = new int[64];

  /** The entry after each entry in the same bucket, or {@code -1} at the end of the chain. */
  private int[] next = new int[64];

  /** The index in {@link #list2} that each entry points to. */
  private int[] entries = new int[64];

  /** The last query that tested each object in {@link #list2}, so objects in several shared cells are tested once. */
  private int[] stamps = new int[64];

  /** Indices in {@link #list2} of objects that cover too many cells to be put in the grid. */
  private int[] large = new int[16];

  private int entryCount;
  private int largeCount;
  private int mask;
  private int stamp;
  private float invCellSize;

  private FlixelCollisionGrid() {}

  /**
   * Tests two objects or groups against each other, using the grid when it pays off.
   *
   * @param obj1 The first object or group.
   * @param obj2 The second object or group.
   * @param notifyCallback Called for each overlapping pair. May be {@code null}.
   * @param processCallback Must return {@code true} for a pair to count. May be {@code null}.
   * @return {@code true} if any pair overlapped.
   */
  public static boolean overlap(@NotNull FlixelBasic obj1,
      @NotNull FlixelBasic obj2,
      @Nullable BiConsumer<FlixelObject, FlixelObject> notifyCallback,
      @Nullable BiPredicate<FlixelObject, FlixelObject> processCallback) {
    FlixelCollisionGrid grid;
    if (depth < contexts.getSize()) {
      grid = contexts.get(depth);
    } else {
      grid = new FlixelCollisionGrid();
      contexts.add(grid);
    }
    depth++;
    try {
      return grid.run(obj1, obj2, notifyCallback, processCallback);
    } finally {
      depth--;
      // Drop the references so killed or removed objects can be garbage collected.
      grid.list1.clear();
      grid.list2.clear();
    }
  }

  private boolean run(FlixelBasic obj1,
      FlixelBasic obj2,
      BiConsumer<FlixelObject, FlixelObject> notifyCallback,
      BiPredicate<FlixelObject, FlixelObject> processCallback) {
    collect(obj1, list1);
    collect(obj2, list2);

    int count1 = list1.getSize();
    int count2 = list2.getSize();
    if (count1 == 0 || count2 == 0) {
      return false;
    }

    FlixelObject[] objects1 = list1.getItems();
    FlixelObject[] objects2 = list2.getItems();
    boolean result = false;

    if ((long) count1 * count2 <= (long) GRID_COST * (count1 + count2)) {
      for (int i = 0; i < count1; i++) {
        FlixelObject fo1 = objects1[i];
        for (int j = 0; j < count2; j++) {
          result |= test(fo1, objects2[j], notifyCallback, processCallback);
        }
      }
      return result;
    }

    build(objects1, count1, objects2, count2);
    for (int i = 0; i < count1; i++) {
      result |= query(objects1[i], objects2, count2, notifyCallback, processCallback);
    }
    return result;
  }

  /**
   * Adds every existing object inside {@code basic} (or {@code basic} itself) that is inside the world bounds to
   * {@code out}.
   *
   * <p>Groups are walked with indexed loops over their backing arrays, so no iterator is created.
   */
  private static void collect(FlixelBasic basic, FlixelArray<FlixelObject> out) {
    if (basic instanceof FlixelGroupable<?> group) {
      FlixelArray<?> members = group.getMembers();
      if (members == null) {
        return;
      }
      Object[] items = members.getItems();
      for (int i = 0, n = members.getSize(); i < n; i++) {
        if (items[i] instanceof FlixelBasic member && member.isExists()) {
          collect(member, out);
        }
      }
      return;
    }

    if (basic instanceof FlixelObject object && object.exists && object.inWorldBounds()) {
      out.add(object);
    }
  }

  /**
   * Runs the narrow phase on one pair: an AABB test, then the process and notify callbacks.
   */
  private static boolean test(FlixelObject fo1,
      FlixelObject fo2,
      BiConsumer<FlixelObject, FlixelObject> notifyCallback,
      BiPredicate<FlixelObject, FlixelObject> processCallback) {
    // A callback earlier in this call may have killed either object, so check again.
    if (fo1 == fo2 || !fo1.exists || !fo2.exists) {
      return false;
    }

    float x1 = fo1.getX();
    float y1 = fo1.getY();
    float x2 = fo2.getX();
    float y2 = fo2.getY();
    boolean overlaps = x1 < x2 + fo2.getWidth()
        && x1 + fo1.getWidth() > x2
        && y1 < y2 + fo2.getHeight()
        && y1 + fo1.getHeight() > y2;
    if (!overlaps) {
      return false;
    }

    if (processCallback != null && !processCallback.test(fo1, fo2)) {
      return false;
    }
    if (notifyCallback != null) {
      notifyCallback.accept(fo1, fo2);
    }
    return true;
  }

  /**
   * Picks a cell size and inserts every object of the second side into the grid.
   */
  private void build(FlixelObject[] objects1, int count1, FlixelObject[] objects2, int count2) {
    // Cells about twice the average object size keep most objects in one to four cells.
    float total = 0f;
    for (int i = 0; i < count1; i++) {
      total += Math.max(objects1[i].getWidth(), objects1[i].getHeight());
    }
    for (int j = 0; j < count2; j++) {
      total += Math.max(objects2[j].getWidth(), objects2[j].getHeight());
    }
    float cellSize = 2f * total / (count1 + count2);
    if (!(cellSize >= 1f)) {
      // Also catches NaN from objects with broken sizes.
      cellSize = 1f;
    }
    invCellSize = 1f / cellSize;

    int bucketCount = Integer.highestOneBit(Math.max(16, count2 * 4 - 1)) << 1;
    if (heads.length < bucketCount) {
      heads = new int[bucketCount];
    }
    Arrays.fill(heads, 0, bucketCount, -1);
    mask = bucketCount - 1;

    if (stamps.length < count2) {
      stamps = new int[Math.max(count2, stamps.length * 2)];
    }
    Arrays.fill(stamps, 0, count2, 0);
    stamp = 0;
    entryCount = 0;
    largeCount = 0;

    // Insert backwards so each bucket's chain lists objects in their original order.
    for (int j = count2 - 1; j >= 0; j--) {
      FlixelObject object = objects2[j];
      float x = object.getX();
      float y = object.getY();
      int cx0 = cell(x);
      int cy0 = cell(y);
      long columns = (long) cell(x + object.getWidth()) - cx0 + 1;
      long rows = (long) cell(y + object.getHeight()) - cy0 + 1;
      if (columns * rows > MAX_CELLS_PER_OBJECT) {
        addLarge(j);
        continue;
      }
      // Loop by offset rather than by cell index so a cell index near Integer.MAX_VALUE cannot loop forever.
      for (int dy = 0; dy < rows; dy++) {
        for (int dx = 0; dx < columns; dx++) {
          insert(hash(cx0 + dx, cy0 + dy), j);
        }
      }
    }
  }

  /**
   * Tests one object of the first side against every object of the second side that shares a cell with it.
   */
  private boolean query(FlixelObject fo1,
      FlixelObject[] objects2,
      int count2,
      BiConsumer<FlixelObject, FlixelObject> notifyCallback,
      BiPredicate<FlixelObject, FlixelObject> processCallback) {
    if (!fo1.exists) {
      return false;
    }

    float x = fo1.getX();
    float y = fo1.getY();
    int cx0 = cell(x);
    int cy0 = cell(y);
    long columns = (long) cell(x + fo1.getWidth()) - cx0 + 1;
    long rows = (long) cell(y + fo1.getHeight()) - cy0 + 1;
    boolean result = false;

    if (columns * rows > MAX_CELLS_PER_OBJECT) {
      // Walking that many cells would cost more than testing everything.
      for (int j = 0; j < count2; j++) {
        result |= test(fo1, objects2[j], notifyCallback, processCallback);
      }
      return result;
    }

    int current = nextStamp(count2);
    for (int dy = 0; dy < rows; dy++) {
      for (int dx = 0; dx < columns; dx++) {
        for (int e = heads[hash(cx0 + dx, cy0 + dy)]; e != -1; e = next[e]) {
          int j = entries[e];
          if (stamps[j] != current) {
            stamps[j] = current;
            result |= test(fo1, objects2[j], notifyCallback, processCallback);
          }
        }
      }
    }
    for (int k = 0; k < largeCount; k++) {
      int j = large[k];
      if (stamps[j] != current) {
        stamps[j] = current;
        result |= test(fo1, objects2[j], notifyCallback, processCallback);
      }
    }
    return result;
  }

  private int nextStamp(int count2) {
    if (stamp == Integer.MAX_VALUE) {
      Arrays.fill(stamps, 0, count2, 0);
      stamp = 0;
    }
    return ++stamp;
  }

  private int cell(float value) {
    return (int) Math.floor(value * invCellSize);
  }

  private int hash(int cx, int cy) {
    int h = cx * 0x9E3779B1 ^ cy * 0x85EBCA77;
    return (h ^ (h >>> 16)) & mask;
  }

  private void insert(int bucket, int index) {
    if (entryCount == entries.length) {
      int capacity = entries.length * 2;
      entries = Arrays.copyOf(entries, capacity);
      next = Arrays.copyOf(next, capacity);
    }
    entries[entryCount] = index;
    next[entryCount] = heads[bucket];
    heads[bucket] = entryCount;
    entryCount++;
  }

  private void addLarge(int index) {
    if (largeCount == large.length) {
      large = Arrays.copyOf(large, large.length * 2);
    }
    large[largeCount++] = index;
  }
}
