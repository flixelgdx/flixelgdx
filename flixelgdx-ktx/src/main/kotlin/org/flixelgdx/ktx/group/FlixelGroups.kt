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
@file:JvmName("FlixelGroups")

package org.flixelgdx.ktx.group

import org.flixelgdx.group.FlixelGroupable

/**
 * Idiomatic Kotlin operators for anything that is a [FlixelGroupable], which covers every group
 * type in the framework.
 *
 * These make membership read like a Kotlin collection (`group += player`, `group -= enemy`,
 * `group[i]`, `for (m in group)`). None of them allocate, except the `iterator()` used by `for`
 * loops, which creates one iterator object per loop. For per-frame iteration prefer an indexed loop
 * using [size] and `group[i]`.
 */

/** Adds [member] to the group, so `group += member` reads like a Kotlin collection. */
operator fun <T> FlixelGroupable<T>.plusAssign(member: T) = add(member)

/** Removes [member] from the group, so `group -= member` mirrors [plusAssign]. */
operator fun <T> FlixelGroupable<T>.minusAssign(member: T) = remove(member)

/** Returns the member at [index], so `group[i]` reads like array access. */
operator fun <T> FlixelGroupable<T>.get(index: Int): T = getMembers()!!.get(index)

/** Iterates the group's members so `for (member in group)` works. */
operator fun <T> FlixelGroupable<T>.iterator(): Iterator<T> = getMembers()!!.iterator()

/** The number of member slots in the group, including empty ones. */
val FlixelGroupable<*>.size: Int
  get() = getMembers()!!.size
