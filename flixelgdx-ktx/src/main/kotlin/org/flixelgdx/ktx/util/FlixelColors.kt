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
@file:JvmName("FlixelColors")

package org.flixelgdx.ktx.util

import org.flixelgdx.util.FlixelColor

/**
 * Idiomatic Kotlin helpers for [FlixelColor].
 *
 * These cover the two things Kotlin users reach for most: unpacking a color into its channels with
 * destructuring, and building a color inline from a packed integer or a hex string. The factory
 * helpers each allocate a new [FlixelColor], so build colors up front rather than in per-frame code.
 */

/** The red channel, enabling `val (r, g, b, a) = color`. */
operator fun FlixelColor.component1(): Float = r

/** The green channel, enabling `val (r, g, b, a) = color`. */
operator fun FlixelColor.component2(): Float = g

/** The blue channel, enabling `val (r, g, b, a) = color`. */
operator fun FlixelColor.component3(): Float = b

/** The alpha channel, enabling `val (r, g, b, a) = color`. */
operator fun FlixelColor.component4(): Float = a

/**
 * Builds a color from this packed `RGBA8888` integer, as in `0xFF00FFFF.toInt().toFlixelColor()`.
 *
 * This allocates a new [FlixelColor].
 */
fun Int.toFlixelColor(): FlixelColor = FlixelColor(this)

/**
 * Builds a color from this hex string, as in `"#ff00ff".toFlixelColor()`.
 *
 * This allocates a new [FlixelColor].
 */
fun String.toFlixelColor(): FlixelColor = FlixelColor().apply { setColor(this@toFlixelColor) }
