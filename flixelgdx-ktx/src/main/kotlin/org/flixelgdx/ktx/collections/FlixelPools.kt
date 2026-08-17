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
@file:JvmName("FlixelPools")

package org.flixelgdx.ktx.collections

import org.flixelgdx.collections.FlixelPool

/**
 * Boilerplate reducers for [FlixelPool].
 *
 * In Java a pool is created by subclassing [FlixelPool] and overriding `newObject()`. In Kotlin the
 * [flixelPool] builder collapses that ceremony into a single lambda, and [use] borrows an object
 * for the duration of a block and frees it automatically, so callers never forget to return it.
 */

/**
 * Creates a pool that builds new objects with [factory].
 *
 * This avoids the anonymous-subclass ceremony the Java API requires:
 *
 * ```
 * val bullets = flixelPool { Bullet() }
 * val enemies = flixelPool(initialCapacity = 64) { Enemy() }
 * ```
 *
 * The [factory] is stored once on the returned pool, so no allocation happens per obtained object
 * beyond the object itself.
 *
 * @param initialCapacity The starting size of the internal free-object storage.
 * @param max The largest number of free objects to retain; objects freed beyond this are discarded.
 * @param factory Produces a fresh, ready-to-use object when the pool has none free.
 */
fun <T : Any> flixelPool(
  initialCapacity: Int = 16,
  max: Int = Int.MAX_VALUE,
  factory: () -> T,
): FlixelPool<T> =
  object : FlixelPool<T>(initialCapacity, max) {
    override fun newObject(): T = factory()
  }

/**
 * Borrows an object for the duration of [block], then returns it to the pool automatically.
 *
 * ```
 * pool.use { bullet -> bullet.fire() }
 * ```
 *
 * The object is freed even if [block] throws. This function is inlined, so the lambda adds no
 * allocation, and it is safe to call every frame.
 *
 * @param block Runs against the borrowed object before it is freed.
 */
inline fun <T : Any> FlixelPool<T>.use(block: (T) -> Unit) {
  val obj = obtain()
  try {
    block(obj)
  } finally {
    free(obj)
  }
}
