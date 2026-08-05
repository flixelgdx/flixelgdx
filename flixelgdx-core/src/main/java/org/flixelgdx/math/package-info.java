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

/**
 * Math value types and helpers owned by FlixelGDX.
 *
 * <p>This package holds the framework's own math surface: the geometric value
 * types ({@link org.flixelgdx.math.FlixelPoint}, {@link org.flixelgdx.math.FlixelRect}),
 * the static math helpers in {@link org.flixelgdx.math.FlixelMathUtil}, and the
 * seedable random generator {@link org.flixelgdx.math.FlixelRandom}. These are
 * clean-room reimplementations that replace the libGDX math utilities the
 * framework used to lean on, so game code never has to touch a third-party math
 * API.
 *
 * <p>The designs here take cues from HaxeFlixel and libGDX (algorithms and API
 * shapes are not copyrightable), but every line is our own. A courtesy credit to
 * both projects is enough; there is no copied source to attribute.
 */
package org.flixelgdx.math;
