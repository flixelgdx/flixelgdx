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
import org.flixelgdx.collections.FlixelBooleanArray
import org.flixelgdx.collections.FlixelByteArray
import org.flixelgdx.collections.FlixelCharArray
import org.flixelgdx.collections.FlixelFloatArray
import org.flixelgdx.collections.FlixelIntArray
import org.flixelgdx.collections.FlixelLongArray
import org.flixelgdx.collections.FlixelShortArray

/**
 * Idiomatic Kotlin operators and factories for FlixelGDX arrays.
 *
 * A few things already work through Java interop and need no help here:
 *
 * - Indexing (`array[i]` and `array[i] = value`), because Kotlin maps Java `get`/`set` to the index
 *   operators automatically.
 * - Membership on the primitive arrays (`value in array`), because each one has a Java `contains`
 *   method. [FlixelCharArray] gets both `in` and iteration from Kotlin's own `CharSequence` support.
 *
 * This file adds the pieces Kotlin cannot infer: `+=`/`-=`, small conveniences, the `flixel*ArrayOf`
 * builders, and typed iterators for `for` loops.
 *
 * The `forEach` helpers are inlined and allocate nothing, so prefer them in per-frame code. The
 * `iterator()` helpers used by `for` loops allocate one iterator object per loop.
 */

/** Creates an array containing [elements], the FlixelGDX counterpart to Kotlin's `arrayOf`. */
fun <T> flixelArrayOf(vararg elements: T): FlixelArray<T> {
  val array = FlixelArray<T>(elements.size)
  for (element in elements) {
    array.add(element)
  }
  return array
}

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

/** Creates a boolean array containing [elements]. */
fun flixelBooleanArrayOf(vararg elements: Boolean): FlixelBooleanArray {
  val array = FlixelBooleanArray(elements.size)
  for (element in elements) {
    array.add(element)
  }
  return array
}

/** Appends [value], so `array += value` reads naturally on the primitive array. */
operator fun FlixelBooleanArray.plusAssign(value: Boolean) = add(value)

/** Removes the first element equal to [value], so `array -= value` mirrors [plusAssign]. */
operator fun FlixelBooleanArray.minusAssign(value: Boolean) {
  removeValue(value)
}

/** The index of the last element, or `-1` when the array is empty. */
val FlixelBooleanArray.lastIndex: Int
  get() = size - 1

/** The range of valid indices for this array. */
val FlixelBooleanArray.indices: IntRange
  get() = 0 until size

/** Iterates every element without allocating; the lambda is inlined. */
inline fun FlixelBooleanArray.forEach(action: (Boolean) -> Unit) {
  for (i in 0 until size) {
    action(get(i))
  }
}

/** Lets `for (value in array)` work. Allocates one iterator per loop; use [forEach] in hot paths. */
operator fun FlixelBooleanArray.iterator(): BooleanIterator =
  object : BooleanIterator() {
    private var index = 0

    override fun hasNext(): Boolean = index < size

    override fun nextBoolean(): Boolean = get(index++)
  }

/** Creates a byte array containing [elements]. */
fun flixelByteArrayOf(vararg elements: Byte): FlixelByteArray {
  val array = FlixelByteArray(elements.size)
  for (element in elements) {
    array.add(element)
  }
  return array
}

/** Appends [value], so `array += value` reads naturally on the primitive array. */
operator fun FlixelByteArray.plusAssign(value: Byte) = add(value)

/** Removes the first element equal to [value], so `array -= value` mirrors [plusAssign]. */
operator fun FlixelByteArray.minusAssign(value: Byte) {
  removeValue(value)
}

/** The index of the last element, or `-1` when the array is empty. */
val FlixelByteArray.lastIndex: Int
  get() = size - 1

/** The range of valid indices for this array. */
val FlixelByteArray.indices: IntRange
  get() = 0 until size

/** Iterates every element without allocating; the lambda is inlined. */
inline fun FlixelByteArray.forEach(action: (Byte) -> Unit) {
  for (i in 0 until size) {
    action(get(i))
  }
}

/** Lets `for (value in array)` work. Allocates one iterator per loop; use [forEach] in hot paths. */
operator fun FlixelByteArray.iterator(): ByteIterator =
  object : ByteIterator() {
    private var index = 0

    override fun hasNext(): Boolean = index < size

    override fun nextByte(): Byte = get(index++)
  }

/** Creates a char array containing [elements]. */
fun flixelCharArrayOf(vararg elements: Char): FlixelCharArray {
  val array = FlixelCharArray(elements.size)
  for (element in elements) {
    array.add(element)
  }
  return array
}

/** Appends [value], so `array += value` reads naturally on the primitive array. */
operator fun FlixelCharArray.plusAssign(value: Char) = add(value)

/** The index of the last element, or `-1` when the array is empty. */
val FlixelCharArray.lastIndex: Int
  get() = size - 1

/** The range of valid indices for this array. */
val FlixelCharArray.indices: IntRange
  get() = 0 until size

/** Creates a short array containing [elements]. */
fun flixelShortArrayOf(vararg elements: Short): FlixelShortArray {
  val array = FlixelShortArray(elements.size)
  for (element in elements) {
    array.add(element)
  }
  return array
}

/** Appends [value], so `array += value` reads naturally on the primitive array. */
operator fun FlixelShortArray.plusAssign(value: Short) = add(value)

/** Removes the first element equal to [value], so `array -= value` mirrors [plusAssign]. */
operator fun FlixelShortArray.minusAssign(value: Short) {
  removeValue(value)
}

/** The index of the last element, or `-1` when the array is empty. */
val FlixelShortArray.lastIndex: Int
  get() = size - 1

/** The range of valid indices for this array. */
val FlixelShortArray.indices: IntRange
  get() = 0 until size

/** Iterates every element without allocating; the lambda is inlined. */
inline fun FlixelShortArray.forEach(action: (Short) -> Unit) {
  for (i in 0 until size) {
    action(get(i))
  }
}

/** Lets `for (value in array)` work. Allocates one iterator per loop; use [forEach] in hot paths. */
operator fun FlixelShortArray.iterator(): ShortIterator =
  object : ShortIterator() {
    private var index = 0

    override fun hasNext(): Boolean = index < size

    override fun nextShort(): Short = get(index++)
  }

/** Creates an int array containing [elements]. */
fun flixelIntArrayOf(vararg elements: Int): FlixelIntArray {
  val array = FlixelIntArray(elements.size)
  for (element in elements) {
    array.add(element)
  }
  return array
}

/** Appends [value], so `array += value` reads naturally on the primitive array. */
operator fun FlixelIntArray.plusAssign(value: Int) = add(value)

/** Removes the first element equal to [value], so `array -= value` mirrors [plusAssign]. */
operator fun FlixelIntArray.minusAssign(value: Int) {
  removeValue(value)
}

/** The index of the last element, or `-1` when the array is empty. */
val FlixelIntArray.lastIndex: Int
  get() = size - 1

/** The range of valid indices for this array. */
val FlixelIntArray.indices: IntRange
  get() = 0 until size

/** Iterates every element without allocating; the lambda is inlined. */
inline fun FlixelIntArray.forEach(action: (Int) -> Unit) {
  for (i in 0 until size) {
    action(get(i))
  }
}

/** Lets `for (value in array)` work. Allocates one iterator per loop; use [forEach] in hot paths. */
operator fun FlixelIntArray.iterator(): IntIterator =
  object : IntIterator() {
    private var index = 0

    override fun hasNext(): Boolean = index < size

    override fun nextInt(): Int = get(index++)
  }

/** Creates a long array containing [elements]. */
fun flixelLongArrayOf(vararg elements: Long): FlixelLongArray {
  val array = FlixelLongArray(elements.size)
  for (element in elements) {
    array.add(element)
  }
  return array
}

/** Appends [value], so `array += value` reads naturally on the primitive array. */
operator fun FlixelLongArray.plusAssign(value: Long) = add(value)

/** Removes the first element equal to [value], so `array -= value` mirrors [plusAssign]. */
operator fun FlixelLongArray.minusAssign(value: Long) {
  removeValue(value)
}

/** The index of the last element, or `-1` when the array is empty. */
val FlixelLongArray.lastIndex: Int
  get() = size - 1

/** The range of valid indices for this array. */
val FlixelLongArray.indices: IntRange
  get() = 0 until size

/** Iterates every element without allocating; the lambda is inlined. */
inline fun FlixelLongArray.forEach(action: (Long) -> Unit) {
  for (i in 0 until size) {
    action(get(i))
  }
}

/** Lets `for (value in array)` work. Allocates one iterator per loop; use [forEach] in hot paths. */
operator fun FlixelLongArray.iterator(): LongIterator =
  object : LongIterator() {
    private var index = 0

    override fun hasNext(): Boolean = index < size

    override fun nextLong(): Long = get(index++)
  }

/** Creates a float array containing [elements]. */
fun flixelFloatArrayOf(vararg elements: Float): FlixelFloatArray {
  val array = FlixelFloatArray(elements.size)
  for (element in elements) {
    array.add(element)
  }
  return array
}

/** Appends [value], so `array += value` reads naturally on the primitive array. */
operator fun FlixelFloatArray.plusAssign(value: Float) = add(value)

/** Removes the first element equal to [value], so `array -= value` mirrors [plusAssign]. */
operator fun FlixelFloatArray.minusAssign(value: Float) {
  removeValue(value)
}

/** The index of the last element, or `-1` when the array is empty. */
val FlixelFloatArray.lastIndex: Int
  get() = size - 1

/** The range of valid indices for this array. */
val FlixelFloatArray.indices: IntRange
  get() = 0 until size

/** Iterates every element without allocating; the lambda is inlined. */
inline fun FlixelFloatArray.forEach(action: (Float) -> Unit) {
  for (i in 0 until size) {
    action(get(i))
  }
}

/** Lets `for (value in array)` work. Allocates one iterator per loop; use [forEach] in hot paths. */
operator fun FlixelFloatArray.iterator(): FloatIterator =
  object : FloatIterator() {
    private var index = 0

    override fun hasNext(): Boolean = index < size

    override fun nextFloat(): Float = get(index++)
  }
