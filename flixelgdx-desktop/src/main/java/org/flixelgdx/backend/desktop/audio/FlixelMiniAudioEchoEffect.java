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
package org.flixelgdx.backend.desktop.audio;

import org.flixelgdx.audio.FlixelEchoEffect;

/**
 * Miniaudio-backed {@link FlixelEchoEffect}.
 *
 * <p>The delay time in seconds is a construction-time parameter for miniaudio's
 * {@code ma_delay_node} and cannot be changed after creation; {@link #setDelay(float)} updates
 * the cached value returned by {@link #getDelay()} but does not retune the native node.
 *
 * <p>The decay factor is live-adjustable at any time through the native bridge.
 *
 * <p>Parameter ID 0 maps to decay.
 */
class FlixelMiniAudioEchoEffect extends FlixelMiniAudioEffect implements FlixelEchoEffect {

  private float delay;
  private float decay;

  FlixelMiniAudioEchoEffect(long nodeHandle, float[] params) {
    super(nodeHandle);
    this.delay = params.length > 0 ? params[0] : 0f;
    this.decay = params.length > 1 ? params[1] : 0f;
  }

  @Override
  public float getDelay() {
    return delay;
  }

  @Override
  public float getDecay() {
    return decay;
  }

  @Override
  public void setDelay(float seconds) {
    delay = seconds;
  }

  @Override
  public void setDecay(float decay) {
    this.decay = decay;
    setParam(0, decay);
  }
}
