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
@file:JvmName("FlixelMaps")

package org.flixelgdx.ktx.collections

import org.flixelgdx.collections.FlixelMap

/**
 * Idiomatic Kotlin operators for [FlixelMap].
 *
 * Reading with `map[key]` already works through Java interop. This file adds writing with
 * `map[key] = value`, the `in` operator, and destructuring iteration so `for ((k, v) in map)`
 * reads like a standard Kotlin map. None of these helpers allocate on their own; the `for` loop
 * reuses the map's own entry iterator.
 */

/** Associates [value] with [key], so `map[key] = value` reads like a Kotlin map assignment. */
operator fun <K : Any, V> FlixelMap<K, V>.set(key: K, value: V?) {
  put(key, value)
}

/** Reports whether [key] is present, enabling `key in map`. */
operator fun <K : Any, V> FlixelMap<K, V>.contains(key: K): Boolean = containsKey(key)

/** Iterates the map's entries so `for (entry in map)` and destructuring loops work. */
operator fun <K, V> FlixelMap<K, V>.iterator(): Iterator<FlixelMap.Entry<K, V>> = entries().iterator()

/** The key half of an entry, enabling `for ((key, value) in map)`. */
operator fun <K, V> FlixelMap.Entry<K, V>.component1(): K = key

/** The value half of an entry, enabling `for ((key, value) in map)`. */
operator fun <K, V> FlixelMap.Entry<K, V>.component2(): V = value

/** Reports whether the map has at least one entry. */
fun FlixelMap<*, *>.isNotEmpty(): Boolean = size != 0
