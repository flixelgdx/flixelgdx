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
 * A live-controllable band-pass filter effect node.
 *
 * <p>Frequencies outside a narrow band around the cutoff are attenuated, keeping only the
 * frequencies near the center. Common uses include phone-call effects, megaphone sounds, and
 * vintage radio simulations.
 *
 * <p>The bandwidth of the band is controlled by the Q (quality) factor: a high Q value produces
 * a narrow, resonant band; a low Q value produces a wider, gentler band.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * FlixelBandPassEffect phone = sound.addBandPass(1200.0, 1.5);
 * // Sharpen the telephone effect:
 * phone.changeQ(1.0);  // Q is now 2.5
 * }</pre>
 */
public interface FlixelBandPassEffect extends FlixelSoundEffect {

  /** No-op sentinel returned when the band-pass filter is unsupported on the current backend. */
  FlixelBandPassEffect NOOP = new FlixelBandPassEffect() {
    public void setParam(int id, float v) {}

    public float getParam(int id) {
      return 0f;
    }

    public void destroy() {}

    public double getCutoff() {
      return 0.0;
    }

    public double getQ() {
      return 0.0;
    }

    public void setCutoff(double v) {}

    public void setQ(double v) {}
  };

  /**
   * Returns the current center frequency of the band.
   *
   * @return Cutoff frequency in hertz.
   */
  double getCutoff();

  /**
   * Returns the current Q (quality) factor.
   *
   * <p>Higher values produce a narrower, more resonant band; lower values widen the pass band.
   *
   * @return Q factor; must be positive.
   */
  double getQ();

  /**
   * Adds {@code amount} to the current cutoff frequency.
   *
   * @param amount Delta in hertz to apply.
   */
  default void changeCutoff(double amount) {
    setCutoff(getCutoff() + amount);
  }

  /**
   * Adds {@code amount} to the current Q factor.
   *
   * @param amount Delta to apply.
   */
  default void changeQ(double amount) {
    setQ(getQ() + amount);
  }

  /**
   * Sets the center frequency of the band.
   *
   * @param hz Cutoff frequency in hertz; must be positive and below the Nyquist frequency.
   */
  void setCutoff(double hz);

  /**
   * Sets the Q (quality) factor.
   *
   * @param q Q factor; must be positive. Typical values are in the range [0.5, 10].
   */
  void setQ(double q);
}
