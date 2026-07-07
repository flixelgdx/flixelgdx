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
package org.flixelgdx.video;

/**
 * Decode quality presets for a {@link FlixelVideo}.
 *
 * <p>Lower presets decode into a smaller pixel buffer, which reduces CPU decode cost,
 * upload bandwidth, and memory. The drawn size on screen never changes; only the
 * source resolution the frames are produced at does, so lower presets look softer.
 *
 * <p>How each platform applies the preset:
 *
 * <ul>
 *   <li>Desktop (libvlc): the decode target is scaled before frames are handed to the
 *       framework, so the savings apply to the whole pipeline.</li>
 *   <li>Web: {@link #FULL} uploads the video element directly to WebGL (usually a
 *       GPU-to-GPU copy). Lower presets route through a downscaled canvas first.</li>
 * </ul>
 */
public enum FlixelVideoQuality {

  /** Decode at the source resolution of the video file. */
  FULL(1f),

  /** Decode at half the source width and height (a quarter of the pixels). */
  HALF(0.5f),

  /** Decode at a quarter of the source width and height. */
  QUARTER(0.25f);

  private final float scale;

  FlixelVideoQuality(float scale) {
    this.scale = scale;
  }

  /**
   * Returns the multiplier applied to the source width and height when decoding.
   *
   * @return Scale factor in {@code (0, 1]}.
   */
  public float getScale() {
    return scale;
  }
}
