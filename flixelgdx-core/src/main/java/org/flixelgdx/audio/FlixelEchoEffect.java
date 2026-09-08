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
 * A live-controllable stereo delay/echo effect node.
 *
 * <p>Delay time and decay can be adjusted at runtime without rebuilding the audio graph.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * FlixelEchoEffect echo = sound.addEcho(0.3f, 0.5f);
 * // Later, lengthen the echo in a large hall:
 * echo.setDelay(0.6f);
 * echo.changeDecay(0.2f);  // decay is now 0.7
 * }</pre>
 */
public interface FlixelEchoEffect extends FlixelSoundEffect {

  /** No-op sentinel returned when echo is unsupported on the current backend. */
  FlixelEchoEffect NOOP = new FlixelEchoEffect() {
    public void setParam(int id, float v) {}

    public float getParam(int id) {
      return 0f;
    }

    public void destroy() {}

    public float getDelay() {
      return 0f;
    }

    public float getDecay() {
      return 0f;
    }

    public void setDelay(float v) {}

    public void setDecay(float v) {}
  };

  /**
   * Returns the current delay time.
   *
   * @return Delay time in seconds.
   */
  float getDelay();

  /**
   * Returns the current decay factor.
   *
   * @return Decay in [0, 1]; lower values fade echoes faster.
   */
  float getDecay();

  /**
   * Adds {@code amount} to the current delay time.
   *
   * @param amount Delta in seconds to apply.
   */
  default void changeDelay(float amount) {
    setDelay(getDelay() + amount);
  }

  /**
   * Adds {@code amount} to the current decay factor.
   *
   * @param amount Delta to apply.
   */
  default void changeDecay(float amount) {
    setDecay(getDecay() + amount);
  }

  /**
   * Sets the delay time.
   *
   * @param delaySeconds Delay time in seconds; must be positive.
   */
  void setDelay(float delaySeconds);

  /**
   * Sets the decay factor for the delayed signal.
   *
   * @param decay Decay in [0, 1]; 0 means no repetition, 1 means infinite sustain.
   */
  void setDecay(float decay);
}
