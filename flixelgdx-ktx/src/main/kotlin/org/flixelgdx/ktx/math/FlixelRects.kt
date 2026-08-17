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
@file:JvmName("FlixelRects")

package org.flixelgdx.ktx.math

import org.flixelgdx.math.FlixelRect

/**
 * Idiomatic Kotlin helpers for [FlixelRect].
 *
 * Point containment already reads well through Java interop: `point in rect` calls
 * `FlixelRect.contains(FlixelVector)`. This file adds destructuring so a rectangle can be unpacked
 * into its four components. None of these helpers allocate.
 */

/** The x component, enabling `val (x, y, width, height) = rect`. */
operator fun FlixelRect.component1(): Float = x

/** The y component, enabling `val (x, y, width, height) = rect`. */
operator fun FlixelRect.component2(): Float = y

/** The width component, enabling `val (x, y, width, height) = rect`. */
operator fun FlixelRect.component3(): Float = width

/** The height component, enabling `val (x, y, width, height) = rect`. */
operator fun FlixelRect.component4(): Float = height
