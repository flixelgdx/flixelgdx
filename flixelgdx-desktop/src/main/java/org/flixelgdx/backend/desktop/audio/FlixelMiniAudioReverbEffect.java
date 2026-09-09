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

import org.flixelgdx.audio.FlixelReverbEffect;

/**
 * Miniaudio-backed {@link FlixelReverbEffect} using the Freeverb algorithm.
 *
 * <p>All parameters are live-adjustable through the native bridge at any time; no reinit is
 * required. The C side maps parameter IDs to the corresponding
 * {@code ma_reverb_node_set_*} functions:
 *
 * <ul>
 *   <li>0 - wet level</li>
 *   <li>1 - dry level</li>
 *   <li>2 - room size</li>
 *   <li>3 - damping</li>
 *   <li>4 - width</li>
 *   <li>5 - freeze mode (0.0 = normal, 1.0 = frozen)</li>
 * </ul>
 */
class FlixelMiniAudioReverbEffect extends FlixelMiniAudioEffect implements FlixelReverbEffect {

  FlixelMiniAudioReverbEffect(long nodeHandle) {
    super(nodeHandle);
  }

  @Override
  public float getWet() {
    return getParam(0);
  }

  @Override
  public float getDry() {
    return getParam(1);
  }

  @Override
  public float getRoomSize() {
    return getParam(2);
  }

  @Override
  public float getDamping() {
    return getParam(3);
  }

  @Override
  public float getWidth() {
    return getParam(4);
  }

  @Override
  public boolean isFrozen() {
    return getParam(5) != 0f;
  }

  @Override
  public void setWet(float wet) {
    setParam(0, wet);
  }

  @Override
  public void setDry(float dry) {
    setParam(1, dry);
  }

  @Override
  public void setRoomSize(float size) {
    setParam(2, size);
  }

  @Override
  public void setDamping(float damping) {
    setParam(3, damping);
  }

  @Override
  public void setWidth(float width) {
    setParam(4, width);
  }

  @Override
  public void setFrozen(boolean frozen) {
    setParam(5, frozen ? 1f : 0f);
  }
}
