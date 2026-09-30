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
package org.flixelgdx.particle;

/**
 * The shape of the area a {@link FlixelEmitter} spawns particles inside.
 *
 * <p>Every shape fills the emitter's bounding box, from its position with its width and height.
 * {@link #CIRCLE} and {@link #RING} become ovals when the width and height differ.
 */
public enum FlixelEmitterShape {

  /** Particles appear anywhere inside the emitter's rectangle. Good for rain, snow, and dust. */
  RECTANGLE,

  /**
   * Particles appear anywhere inside the circle that fits the emitter's box, spread evenly over its
   * area. Good for puffs, magic auras, and explosions that start with some size.
   */
  CIRCLE,

  /**
   * Particles appear in a band just inside the edge of the circle that fits the emitter's box. The
   * band's width is the emitter's ring thickness; a thickness of zero puts every particle exactly on
   * the edge. Good for shockwaves, portals, and halos.
   */
  RING
}
