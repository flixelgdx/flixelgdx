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
@file:JvmName("FlixelVectors")

package org.flixelgdx.ktx.math

import org.flixelgdx.math.FlixelVector

/**
 * Idiomatic Kotlin operators for [FlixelVector].
 *
 * There are two families here, and the difference matters for performance:
 *
 * - The compound-assignment operators (`+=`, `-=`, `*=`) mutate the vector in place and allocate
 *   nothing. Prefer these in per-frame code.
 * - The binary operators (`+`, `-`, `*`, unary `-`) each return a brand-new [FlixelVector]. They
 *   read cleanly but allocate, so keep them out of hot loops.
 *
 * Destructuring (`val (x, y) = vector`) is also provided and allocates nothing.
 */

/** Adds [other] into this vector in place. Allocates nothing. */
operator fun FlixelVector.plusAssign(other: FlixelVector) {
  add(other)
}

/** Subtracts [other] from this vector in place. Allocates nothing. */
operator fun FlixelVector.minusAssign(other: FlixelVector) {
  subtract(other)
}

/** Scales this vector in place by [factor]. Allocates nothing. */
operator fun FlixelVector.timesAssign(factor: Float) {
  scale(factor)
}

/**
 * Returns a new vector equal to this one plus [other].
 *
 * This allocates a new [FlixelVector]; use [plusAssign] in per-frame code.
 */
operator fun FlixelVector.plus(other: FlixelVector): FlixelVector = FlixelVector(x + other.x, y + other.y)

/**
 * Returns a new vector equal to this one minus [other].
 *
 * This allocates a new [FlixelVector]; use [minusAssign] in per-frame code.
 */
operator fun FlixelVector.minus(other: FlixelVector): FlixelVector = FlixelVector(x - other.x, y - other.y)

/**
 * Returns a new vector equal to this one scaled by [factor].
 *
 * This allocates a new [FlixelVector]; use [timesAssign] in per-frame code.
 */
operator fun FlixelVector.times(factor: Float): FlixelVector = FlixelVector(x * factor, y * factor)

/**
 * Returns a new vector pointing in the opposite direction.
 *
 * This allocates a new [FlixelVector]; call `negate()` to flip in place instead.
 */
operator fun FlixelVector.unaryMinus(): FlixelVector = FlixelVector(-x, -y)

/** The x component, enabling `val (x, y) = vector`. */
operator fun FlixelVector.component1(): Float = x

/** The y component, enabling `val (x, y) = vector`. */
operator fun FlixelVector.component2(): Float = y
