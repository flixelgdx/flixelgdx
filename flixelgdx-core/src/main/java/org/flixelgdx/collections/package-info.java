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
 * Collection and pooling types owned by FlixelGDX.
 *
 * <p>This package is the framework's own replacement for the libGDX collection
 * and pooling utilities the framework used to depend on. It holds object pooling
 * ({@link org.flixelgdx.collections.FlixelPool},
 * {@link org.flixelgdx.collections.FlixelPoolable}) and, as Phase 1 of the
 * migration continues, the growable arrays, maps, and sets that game code and
 * the framework itself iterate every frame.
 *
 * <p>Everything here is built for the framework's no-per-frame-allocation rule:
 * backing arrays are reused, iteration is index based, and growth is amortized.
 * The designs take cues from HaxeFlixel and libGDX (algorithms are not
 * copyrightable), but every line is our own clean-room implementation.
 */
package org.flixelgdx.collections;
