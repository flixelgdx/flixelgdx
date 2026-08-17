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
@file:JvmName("FlixelSets")

package org.flixelgdx.ktx.collections

import org.flixelgdx.collections.FlixelIntSet
import org.flixelgdx.collections.FlixelLongSet
import org.flixelgdx.collections.FlixelSet

/**
 * Idiomatic Kotlin operators and factories for the FlixelGDX set types.
 *
 * Membership already works through the sets' own hashing: `value in set` calls each set's Java
 * `contains`, which is a constant-time lookup. This file adds `+=`/`-=`, the `flixel*SetOf`
 * builders, and iteration helpers.
 *
 * [FlixelSet] is `Iterable`, so `for (value in set)` works directly. The primitive sets are backed
 * by open-addressed hashing with no stable index, so instead of a `for` loop they expose an inlined,
 * allocation-free [forEach] that reuses each set's own iterator.
 */

/** Creates a set containing [elements], the FlixelGDX counterpart to Kotlin's `setOf`. */
fun <T : Any> flixelSetOf(vararg elements: T): FlixelSet<T> {
  val set = FlixelSet<T>(elements.size)
  for (element in elements) {
    set.add(element)
  }
  return set
}

/** Adds [value] to the set, so `set += value` reads like a Kotlin collection. */
operator fun <T : Any> FlixelSet<T>.plusAssign(value: T) {
  add(value)
}

/** Removes [value] from the set, so `set -= value` mirrors [plusAssign]. */
operator fun <T : Any> FlixelSet<T>.minusAssign(value: T) {
  remove(value)
}

/** Reports whether the set has at least one element. */
fun FlixelSet<*>.isNotEmpty(): Boolean = size != 0

/** Creates an int set containing [elements]. */
fun flixelIntSetOf(vararg elements: Int): FlixelIntSet {
  val set = FlixelIntSet(elements.size)
  for (element in elements) {
    set.add(element)
  }
  return set
}

/** Adds [value] to the set, so `set += value` reads like a Kotlin collection. */
operator fun FlixelIntSet.plusAssign(value: Int) {
  add(value)
}

/** Removes [value] from the set, so `set -= value` mirrors [plusAssign]. */
operator fun FlixelIntSet.minusAssign(value: Int) {
  remove(value)
}

/** Reports whether the set has at least one element. */
fun FlixelIntSet.isNotEmpty(): Boolean = size != 0

/**
 * Iterates every element without allocating, reusing the set's own iterator. The lambda is inlined,
 * so this is safe to call every frame.
 */
inline fun FlixelIntSet.forEach(action: (Int) -> Unit) {
  val iterator = iterator()
  while (iterator.hasNext) {
    action(iterator.next())
  }
}

/** Creates a long set containing [elements]. */
fun flixelLongSetOf(vararg elements: Long): FlixelLongSet {
  val set = FlixelLongSet(elements.size)
  for (element in elements) {
    set.add(element)
  }
  return set
}

/** Adds [value] to the set, so `set += value` reads like a Kotlin collection. */
operator fun FlixelLongSet.plusAssign(value: Long) {
  add(value)
}

/** Removes [value] from the set, so `set -= value` mirrors [plusAssign]. */
operator fun FlixelLongSet.minusAssign(value: Long) {
  remove(value)
}

/** Reports whether the set has at least one element. */
fun FlixelLongSet.isNotEmpty(): Boolean = size != 0

/**
 * Iterates every element without allocating, reusing the set's own iterator. The lambda is inlined,
 * so this is safe to call every frame.
 */
inline fun FlixelLongSet.forEach(action: (Long) -> Unit) {
  val iterator = iterator()
  while (iterator.hasNext) {
    action(iterator.next())
  }
}
