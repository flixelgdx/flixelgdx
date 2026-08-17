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
@file:JvmName("FlixelArrays")

package org.flixelgdx.ktx.collections

import org.flixelgdx.collections.FlixelArray
import org.flixelgdx.collections.FlixelFloatArray
import org.flixelgdx.collections.FlixelIntArray

/**
 * Idiomatic Kotlin operators for FlixelGDX arrays.
 *
 * Indexing (`array[i]` and `array[i] = value`) already works out of the box, because Kotlin maps
 * Java `get`/`set` methods to the index operators automatically. This file adds the pieces Kotlin
 * cannot infer from the Java side: `+=`/`-=`, the `in` operator, and small conveniences.
 *
 * None of these helpers allocate. The `iterator()` helpers for the primitive arrays do allocate a
 * single iterator object per loop, so prefer the `forEach` helpers or an indexed loop in per-frame
 * code.
 */

/** Appends [value] to the array, so `array += value` reads like a normal Kotlin collection. */
operator fun <T> FlixelArray<T>.plusAssign(value: T?) = add(value)

/** Removes the first element equal to [value], so `array -= value` mirrors [plusAssign]. */
operator fun <T> FlixelArray<T>.minusAssign(value: T?) {
  removeValue(value, false)
}

/** Reports whether [value] is present using `equals`, enabling `value in array`. */
operator fun <T> FlixelArray<T>.contains(value: T?): Boolean = contains(value, false)

/** Reports whether the array has at least one element. */
fun FlixelArray<*>.isNotEmpty(): Boolean = size != 0

/** The index of the last element, or `-1` when the array is empty. */
val FlixelArray<*>.lastIndex: Int
  get() = size - 1

/** The range of valid indices, handy for `for (i in array.indices)`. */
val FlixelArray<*>.indices: IntRange
  get() = 0 until size

/** Appends [value], so `array += value` reads naturally on the primitive array. */
operator fun FlixelIntArray.plusAssign(value: Int) = add(value)

/** The index of the last element, or `-1` when the array is empty. */
val FlixelIntArray.lastIndex: Int
  get() = size - 1

/** The range of valid indices for this array. */
val FlixelIntArray.indices: IntRange
  get() = 0 until size

/**
 * Iterates every element without allocating, so `array.forEach { ... }` stays safe in per-frame
 * code. The lambda is inlined, so no iterator or closure object is created.
 */
inline fun FlixelIntArray.forEach(action: (Int) -> Unit) {
  for (i in 0 until size) {
    action(get(i))
  }
}

/**
 * Lets `for (value in array)` work on the primitive array. This allocates one iterator per loop;
 * use [forEach] or an indexed loop in hot paths.
 */
operator fun FlixelIntArray.iterator(): IntIterator =
  object : IntIterator() {
    private var index = 0

    override fun hasNext(): Boolean = index < size

    override fun nextInt(): Int = get(index++)
  }

/** Appends [value], so `array += value` reads naturally on the primitive array. */
operator fun FlixelFloatArray.plusAssign(value: Float) = add(value)

/** The index of the last element, or `-1` when the array is empty. */
val FlixelFloatArray.lastIndex: Int
  get() = size - 1

/** The range of valid indices for this array. */
val FlixelFloatArray.indices: IntRange
  get() = 0 until size

/**
 * Iterates every element without allocating, so `array.forEach { ... }` stays safe in per-frame
 * code. The lambda is inlined, so no iterator or closure object is created.
 */
inline fun FlixelFloatArray.forEach(action: (Float) -> Unit) {
  for (i in 0 until size) {
    action(get(i))
  }
}

/**
 * Lets `for (value in array)` work on the primitive array. This allocates one iterator per loop;
 * use [forEach] or an indexed loop in hot paths.
 */
operator fun FlixelFloatArray.iterator(): FloatIterator =
  object : FloatIterator() {
    private var index = 0

    override fun hasNext(): Boolean = index < size

    override fun nextFloat(): Float = get(index++)
  }
