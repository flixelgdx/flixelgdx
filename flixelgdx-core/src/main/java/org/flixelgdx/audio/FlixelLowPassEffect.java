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

import org.jetbrains.annotations.NotNull;

/**
 * A live-controllable low-pass filter effect node.
 *
 * <p>The cutoff can be read back via {@link #getCutoff()} and adjusted by a delta via
 * {@link #changeCutoff(double)} without needing to combine a getter and setter manually.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * FlixelLowPassEffect muffle = sound.addLowPassMuffle(8000.0);
 * // Smoothly tighten the filter as the player goes deeper underground:
 * muffle.changeCutoff(-500.0);  // now 7500 Hz
 * }</pre>
 */
public interface FlixelLowPassEffect extends FlixelSoundEffect {

  /** No-op sentinel returned when no audio backend is available. */
  FlixelLowPassEffect NOOP = new FlixelLowPassEffect() {
    public void attachToUpstreamSound(@NotNull FlixelSound u, int b) {}

    public void attachToUpstreamNode(@NotNull FlixelSoundEffect u, int b) {}

    public void detach(int b) {}

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
   * <p>Frequencies above the cutoff are progressively attenuated.
   *
   * @param hz Cutoff frequency in hertz; must be positive and below the Nyquist frequency.
   */
  void setCutoff(double hz);
}
