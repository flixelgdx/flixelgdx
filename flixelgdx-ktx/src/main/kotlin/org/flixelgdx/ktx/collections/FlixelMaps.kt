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

import org.flixelgdx.collections.FlixelIdentityMap
import org.flixelgdx.collections.FlixelIntMap
import org.flixelgdx.collections.FlixelLongMap
import org.flixelgdx.collections.FlixelMap
import org.flixelgdx.collections.FlixelObjectFloatMap
import org.flixelgdx.collections.FlixelObjectIntMap

/**
 * Idiomatic Kotlin operators and factories for the FlixelGDX map types.
 *
 * Reading with `map[key]` already works through Java interop wherever the map has a single-argument
 * getter (the object-keyed and primitive-keyed maps). Membership uses `key in map`, which delegates
 * to each map's constant-time `containsKey`; iteration reuses the map's own entry iterator. This
 * file adds writing with `map[key] = value`, the `in` operator, the `flixel*MapOf` builders, and
 * destructuring so `for ((key, value) in map)` reads like a standard Kotlin map.
 *
 * The primitive-value maps ([FlixelObjectIntMap], [FlixelObjectFloatMap]) do not support `map[key]`
 * reads, since their getter needs an explicit default; call `get(key, default)` for those.
 */

/** Creates a map containing [pairs], the FlixelGDX counterpart to Kotlin's `mapOf`. */
fun <K : Any, V> flixelMapOf(vararg pairs: Pair<K, V>): FlixelMap<K, V> {
  val map = FlixelMap<K, V>(pairs.size)
  for ((key, value) in pairs) {
    map.put(key, value)
  }
  return map
}

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

/** Creates an identity map containing [pairs], where keys are compared by reference. */
fun <K : Any, V> flixelIdentityMapOf(vararg pairs: Pair<K, V>): FlixelIdentityMap<K, V> {
  val map = FlixelIdentityMap<K, V>(pairs.size)
  for ((key, value) in pairs) {
    map.put(key, value)
  }
  return map
}

/** Associates [value] with [key], so `map[key] = value` reads like a Kotlin map assignment. */
operator fun <K : Any, V> FlixelIdentityMap<K, V>.set(key: K, value: V?) {
  put(key, value)
}

/** Reports whether [key] is present, enabling `key in map`. */
operator fun <K : Any, V> FlixelIdentityMap<K, V>.contains(key: K): Boolean = containsKey(key)

/** Iterates the map's entries so `for (entry in map)` and destructuring loops work. */
operator fun <K, V> FlixelIdentityMap<K, V>.iterator(): Iterator<FlixelIdentityMap.Entry<K, V>> =
  entries().iterator()

/** The key half of an entry, enabling `for ((key, value) in map)`. */
operator fun <K, V> FlixelIdentityMap.Entry<K, V>.component1(): K = key

/** The value half of an entry, enabling `for ((key, value) in map)`. */
operator fun <K, V> FlixelIdentityMap.Entry<K, V>.component2(): V = value

/** Reports whether the map has at least one entry. */
fun FlixelIdentityMap<*, *>.isNotEmpty(): Boolean = size != 0

/** Creates an int-keyed map containing [pairs]. */
fun <V> flixelIntMapOf(vararg pairs: Pair<Int, V>): FlixelIntMap<V> {
  val map = FlixelIntMap<V>(pairs.size)
  for ((key, value) in pairs) {
    map.put(key, value)
  }
  return map
}

/** Associates [value] with [key], so `map[key] = value` reads like a Kotlin map assignment. */
operator fun <V> FlixelIntMap<V>.set(key: Int, value: V?) {
  put(key, value)
}

/** Reports whether [key] is present, enabling `key in map`. */
operator fun FlixelIntMap<*>.contains(key: Int): Boolean = containsKey(key)

/** Iterates the map's entries so `for (entry in map)` and destructuring loops work. */
operator fun <V> FlixelIntMap<V>.iterator(): Iterator<FlixelIntMap.Entry<V>> = entries().iterator()

/** The key half of an entry, enabling `for ((key, value) in map)`. */
operator fun <V> FlixelIntMap.Entry<V>.component1(): Int = key

/** The value half of an entry, enabling `for ((key, value) in map)`. */
operator fun <V> FlixelIntMap.Entry<V>.component2(): V = value

/** Reports whether the map has at least one entry. */
fun FlixelIntMap<*>.isNotEmpty(): Boolean = size != 0

/** Creates a long-keyed map containing [pairs]. */
fun <V> flixelLongMapOf(vararg pairs: Pair<Long, V>): FlixelLongMap<V> {
  val map = FlixelLongMap<V>(pairs.size)
  for ((key, value) in pairs) {
    map.put(key, value)
  }
  return map
}

/** Associates [value] with [key], so `map[key] = value` reads like a Kotlin map assignment. */
operator fun <V> FlixelLongMap<V>.set(key: Long, value: V?) {
  put(key, value)
}

/** Reports whether [key] is present, enabling `key in map`. */
operator fun FlixelLongMap<*>.contains(key: Long): Boolean = containsKey(key)

/** Iterates the map's entries so `for (entry in map)` and destructuring loops work. */
operator fun <V> FlixelLongMap<V>.iterator(): Iterator<FlixelLongMap.Entry<V>> = entries().iterator()

/** The key half of an entry, enabling `for ((key, value) in map)`. */
operator fun <V> FlixelLongMap.Entry<V>.component1(): Long = key

/** The value half of an entry, enabling `for ((key, value) in map)`. */
operator fun <V> FlixelLongMap.Entry<V>.component2(): V = value

/** Reports whether the map has at least one entry. */
fun FlixelLongMap<*>.isNotEmpty(): Boolean = size != 0

/** Creates a map from object keys to `int` values containing [pairs]. */
fun <K : Any> flixelObjectIntMapOf(vararg pairs: Pair<K, Int>): FlixelObjectIntMap<K> {
  val map = FlixelObjectIntMap<K>(pairs.size)
  for ((key, value) in pairs) {
    map.put(key, value)
  }
  return map
}

/** Associates [value] with [key], so `map[key] = value` reads like a Kotlin map assignment. */
operator fun <K : Any> FlixelObjectIntMap<K>.set(key: K, value: Int) {
  put(key, value)
}

/** Reports whether [key] is present, enabling `key in map`. */
operator fun <K : Any> FlixelObjectIntMap<K>.contains(key: K): Boolean = containsKey(key)

/** Iterates the map's entries so `for (entry in map)` and destructuring loops work. */
operator fun <K> FlixelObjectIntMap<K>.iterator(): Iterator<FlixelObjectIntMap.Entry<K>> =
  entries().iterator()

/** The key half of an entry, enabling `for ((key, value) in map)`. */
operator fun <K> FlixelObjectIntMap.Entry<K>.component1(): K = key

/** The value half of an entry, enabling `for ((key, value) in map)`. */
operator fun <K> FlixelObjectIntMap.Entry<K>.component2(): Int = value

/** Reports whether the map has at least one entry. */
fun FlixelObjectIntMap<*>.isNotEmpty(): Boolean = size != 0

/** Creates a map from object keys to `float` values containing [pairs]. */
fun <K : Any> flixelObjectFloatMapOf(vararg pairs: Pair<K, Float>): FlixelObjectFloatMap<K> {
  val map = FlixelObjectFloatMap<K>(pairs.size)
  for ((key, value) in pairs) {
    map.put(key, value)
  }
  return map
}

/** Associates [value] with [key], so `map[key] = value` reads like a Kotlin map assignment. */
operator fun <K : Any> FlixelObjectFloatMap<K>.set(key: K, value: Float) {
  put(key, value)
}

/** Reports whether [key] is present, enabling `key in map`. */
operator fun <K : Any> FlixelObjectFloatMap<K>.contains(key: K): Boolean = containsKey(key)

/** Iterates the map's entries so `for (entry in map)` and destructuring loops work. */
operator fun <K> FlixelObjectFloatMap<K>.iterator(): Iterator<FlixelObjectFloatMap.Entry<K>> =
  entries().iterator()

/** The key half of an entry, enabling `for ((key, value) in map)`. */
operator fun <K> FlixelObjectFloatMap.Entry<K>.component1(): K = key

/** The value half of an entry, enabling `for ((key, value) in map)`. */
operator fun <K> FlixelObjectFloatMap.Entry<K>.component2(): Float = value

/** Reports whether the map has at least one entry. */
fun FlixelObjectFloatMap<*>.isNotEmpty(): Boolean = size != 0
