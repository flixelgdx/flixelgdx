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
package org.flixelgdx.audio;

/**
 * A live-controllable high-pass filter effect node.
 *
 * <p>Frequencies below the cutoff are progressively attenuated, producing a thin or airy quality.
 * Common uses include simulating wind at height, radio transmissions, or footsteps heard through
 * a wall.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * FlixelHighPassEffect thin = sound.addHighPass(500.0);
 * // Raise the cutoff for a tinnier, more distant quality:
 * thin.changeCutoff(300.0);  // now 800 Hz
 * }</pre>
 */
public interface FlixelHighPassEffect extends FlixelSoundEffect {

  /** No-op sentinel returned when the high-pass filter is unsupported on the current backend. */
  FlixelHighPassEffect NOOP = new FlixelHighPassEffect() {
    public void setParam(int id, float v) {}

    public float getParam(int id) {
      return 0f;
    }

    public void destroy() {}

    public double getCutoff() {
      return 0.0;
    }

    public void setCutoff(double v) {}
  };

  /**
   * Returns the current filter cutoff frequency.
   *
   * @return Cutoff frequency in hertz.
   */
  double getCutoff();

  /**
   * Adds {@code amount} to the current cutoff frequency.
   *
   * @param amount Delta in hertz to apply.
   */
  default void changeCutoff(double amount) {
    setCutoff(getCutoff() + amount);
  }

  /**
   * Sets the filter cutoff frequency.
   *
   * <p>Frequencies below the cutoff are progressively attenuated.
   *
   * @param hz Cutoff frequency in hertz; must be positive and below the Nyquist frequency.
   */
  void setCutoff(double hz);
}
