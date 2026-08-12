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

import org.jetbrains.annotations.NotNull;

/**
 * A seedable pseudo-random number generator with game-friendly helpers.
 *
 * <p>The framework exposes one globally as {@code Flixel.random}, but it is an
 * ordinary object you can create as many of as you like. Being instance based
 * and seedable is the whole point: give two runs the same seed and they produce
 * the same sequence, which is what makes deterministic replays, seeded
 * procedural generation, and reproducible tests possible.
 *
 * <p>The generator is an {@code xorshift64*}, a small, fast, and well-distributed
 * algorithm. Because the math is fully specified here rather than delegated to a
 * platform's {@link java.util.Random}, the same seed yields the same numbers on
 * desktop, web, and mobile alike.
 *
 * <p>Example:
 *
 * <pre>{@code
 * FlixelRandom rng = new FlixelRandom(1234L);
 * int roll = rng.nextInt(1, 6);       // a six-sided die
 * boolean crit = rng.nextBool(0.05f); // 5% chance
 * String loot = rng.pick(lootTable);  // a random element
 * }</pre>
 *
 * <p>This class is not thread safe; give each thread its own instance.
 */
public class FlixelRandom {

  private static final long MULTIPLIER = 0x2545F4914F6CDD1DL;

  // A non-zero constant used when a caller seeds the generator with 0, since
  // xorshift can never recover from an all-zero state.
  private static final long ZERO_SEED_REPLACEMENT = 0x9E3779B97F4A7C15L;

  private long seed;
  private long state;

  /**
   * Creates a generator seeded from the current time, so each run differs.
   */
  public FlixelRandom() {
    this(System.nanoTime());
  }

  /**
   * Creates a generator with a fixed seed, producing a repeatable sequence.
   *
   * @param seed The starting seed. Any {@code long} is accepted, including 0.
   */
  public FlixelRandom(long seed) {
    setSeed(seed);
  }

  /**
   * Returns the next raw 64-bit value in the sequence.
   *
   * <p>Most callers want a bounded helper instead; this is the primitive the
   * others build on.
   *
   * @return A pseudo-random {@code long} spanning the full 64-bit range.
   */
  public long nextLong() {
    long x = state;
    x ^= x >>> 12;
    x ^= x << 25;
    x ^= x >>> 27;
    state = x;
    return x * MULTIPLIER;
  }

  /**
   * Returns the next pseudo-random {@code int} across the full 32-bit range.
   *
   * @return A pseudo-random {@code int}, possibly negative.
   */
  public int nextInt() {
    return (int) (nextLong() >>> 32);
  }

  /**
   * Returns a pseudo-random {@code int} in the range {@code [0, bound)}.
   *
   * @param bound The exclusive upper bound; must be positive.
   * @return A value at least 0 and less than {@code bound}.
   * @throws IllegalArgumentException If {@code bound} is not positive.
   */
  public int nextInt(int bound) {
    if (bound <= 0) {
      throw new IllegalArgumentException("bound must be positive: " + bound);
    }
    // Multiply-high: scale the top 32 random bits into [0, bound) with no
    // division and only a slight, negligible bias for game use.
    long bits = nextLong() >>> 32;
    return (int) ((bits * bound) >>> 32);
  }

  /**
   * Returns a pseudo-random {@code int} in the inclusive range
   * {@code [min, max]}.
   *
   * @param min The inclusive lower bound.
   * @param max The inclusive upper bound; must be greater than or equal to
   *     {@code min}.
   * @return A value between {@code min} and {@code max}, inclusive.
   * @throws IllegalArgumentException If {@code max} is less than {@code min}.
   */
  public int nextInt(int min, int max) {
    if (max < min) {
      throw new IllegalArgumentException("max (" + max + ") < min (" + min + ")");
    }
    // Use a long span so a full-width range like [MIN_VALUE, MAX_VALUE] cannot
    // overflow.
    long span = (long) max - min + 1L;
    long bits = nextLong() >>> 32;
    return (int) (min + ((bits * span) >>> 32));
  }

  /**
   * Returns a pseudo-random {@code float} in the range {@code [0, 1)}.
   *
   * @return A value at least 0 and less than 1.
   */
  public float nextFloat() {
    // Take the top 24 bits, matching a float's mantissa precision.
    return (nextLong() >>> 40) * (1.0f / (1 << 24));
  }

  /**
   * Returns a pseudo-random {@code float} in the range {@code [min, max)}.
   *
   * @param min The inclusive lower bound.
   * @param max The exclusive upper bound.
   * @return A value at least {@code min} and less than {@code max}.
   */
  public float nextFloat(float min, float max) {
    return min + nextFloat() * (max - min);
  }

  /**
   * Returns {@code true} or {@code false} with equal probability.
   *
   * @return A pseudo-random boolean.
   */
  public boolean nextBool() {
    return (nextLong() & 1L) != 0L;
  }

  /**
   * Returns {@code true} with the given probability.
   *
   * @param chance The probability of {@code true}, from 0 (never) to 1 (always).
   * @return {@code true} with probability {@code chance}.
   */
  public boolean nextBool(float chance) {
    return nextFloat() < chance;
  }

  /**
   * Returns -1 or 1 with equal probability.
   *
   * <p>Handy for randomizing a direction or flipping a value's sign.
   *
   * @return Either -1 or 1.
   */
  public int sign() {
    return nextBool() ? 1 : -1;
  }

  /**
   * Returns a random element from the given array.
   *
   * @param items The array to choose from; must not be empty.
   * @param <T> The element type.
   * @return A randomly chosen element.
   * @throws IllegalArgumentException If {@code items} is empty.
   */
  public <T> T pick(@NotNull T[] items) {
    if (items.length == 0) {
      throw new IllegalArgumentException("cannot pick from an empty array");
    }
    return items[nextInt(items.length)];
  }

  /**
   * Shuffles the given array in place using an unbiased Fisher-Yates pass.
   *
   * @param items The array to shuffle; may be empty.
   * @param <T> The element type.
   */
  public <T> void shuffle(@NotNull T[] items) {
    for (int i = items.length - 1; i > 0; i--) {
      int j = nextInt(i + 1);
      T tmp = items[i];
      items[i] = items[j];
      items[j] = tmp;
    }
  }

  /**
   * Reseeds the generator, restarting its sequence from a known point.
   *
   * @param seed The new seed. A seed of 0 is remapped internally so the
   *     generator never gets stuck in an all-zero state.
   */
  public void setSeed(long seed) {
    this.seed = seed;
    this.state = seed == 0L ? ZERO_SEED_REPLACEMENT : seed;
  }

  /**
   * Returns the seed this generator was last seeded with.
   *
   * @return The current seed (the value passed to the constructor or
   *     {@link #setSeed(long)}, before any zero remapping).
   */
  public long getSeed() {
    return seed;
  }
}
