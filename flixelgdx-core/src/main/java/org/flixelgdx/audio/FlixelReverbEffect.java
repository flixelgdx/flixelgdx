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
 * A live-controllable reverb effect node.
 *
 * <p>All setters take effect immediately without rebuilding the audio graph. Corresponding
 * {@code change*()} methods apply a delta to the current value, so callers do not need to
 * manually combine a getter with a setter.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * FlixelReverbEffect reverb = sound.addReverb(0.4f);
 * // Later, when the player enters a cave:
 * reverb.setRoomSize(0.9f);
 * reverb.changeWet(0.3f);  // wet is now 0.7
 * }</pre>
 */
public interface FlixelReverbEffect extends FlixelSoundEffect {

  /** No-op sentinel returned when no audio backend is available. */
  FlixelReverbEffect NOOP = new FlixelReverbEffect() {
    public void attachToUpstreamSound(@NotNull FlixelSound u, int b) {}

    public void attachToUpstreamNode(@NotNull FlixelSoundEffect u, int b) {}

    public void detach(int b) {}

    public void destroy() {}

    public float getWet() {
      return 0f;
    }

    public float getDry() {
      return 0f;
    }

    public float getRoomSize() {
      return 0f;
    }

    public float getDamping() {
      return 0f;
    }

    public float getWidth() {
      return 0f;
    }

    public boolean isFrozen() {
      return false;
    }

    public void setWet(float v) {}

    public void setDry(float v) {}

    public void setRoomSize(float v) {}

    public void setDamping(float v) {}

    public void setWidth(float v) {}

    public void setFrozen(boolean v) {}
  };

  /**
   * Returns the current wet (processed) signal level.
   *
   * @return Level in [0, 1].
   */
  float getWet();

  /**
   * Returns the current dry (unprocessed) signal level.
   *
   * @return Level in [0, 1].
   */
  float getDry();

  /**
   * Returns the current simulated room size.
   *
   * @return Room size in [0, 1].
   */
  float getRoomSize();

  /**
   * Returns the current high-frequency damping amount.
   *
   * @return Damping in [0, 1].
   */
  float getDamping();

  /**
   * Returns the current stereo width of the reverb tail.
   *
   * @return Width in [0, 1].
   */
  float getWidth();

  /**
   * Returns whether the reverb tail is currently frozen.
   *
   * @return {@code true} if the tail is recirculating indefinitely.
   */
  boolean isFrozen();

  /**
   * Adds {@code amount} to the current wet level, clamped to [0, 1].
   *
   * @param amount Delta to apply.
   */
  default void changeWet(float amount) {
    setWet(getWet() + amount);
  }

  /**
   * Adds {@code amount} to the current dry level, clamped to [0, 1].
   *
   * @param amount Delta to apply.
   */
  default void changeDry(float amount) {
    setDry(getDry() + amount);
  }

  /**
   * Adds {@code amount} to the current room size, clamped to [0, 1].
   *
   * @param amount Delta to apply.
   */
  default void changeRoomSize(float amount) {
    setRoomSize(getRoomSize() + amount);
  }

  /**
   * Adds {@code amount} to the current damping, clamped to [0, 1].
   *
   * @param amount Delta to apply.
   */
  default void changeDamping(float amount) {
    setDamping(getDamping() + amount);
  }

  /**
   * Adds {@code amount} to the current width, clamped to [0, 1].
   *
   * @param amount Delta to apply.
   */
  default void changeWidth(float amount) {
    setWidth(getWidth() + amount);
  }

  /**
   * Sets the wet (processed) signal level.
   *
   * @param wet Level in [0, 1].
   */
  void setWet(float wet);

  /**
   * Sets the dry (unprocessed) signal level.
   *
   * @param dry Level in [0, 1].
   */
  void setDry(float dry);

  /**
   * Sets the simulated room size.
   *
   * @param size Room size in [0, 1]; larger values produce longer reverb tails.
   */
  void setRoomSize(float size);

  /**
   * Sets the high-frequency damping amount.
   *
   * @param damping Damping in [0, 1]; higher values absorb treble faster.
   */
  void setDamping(float damping);

  /**
   * Sets the stereo width of the reverb tail.
   *
   * @param width Width in [0, 1]; 0 is mono, 1 is full stereo spread.
   */
  void setWidth(float width);

  /**
   * Freezes or unfreezes the reverb tail.
   *
   * <p>When frozen, the tail recirculates indefinitely, producing an infinite-sustain effect.
   *
   * @param frozen {@code true} to freeze, {@code false} for normal decay.
   */
  void setFrozen(boolean frozen);
}
