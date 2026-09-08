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

import org.flixelgdx.audio.FlixelSoundEffect;

/**
 * Base class for all miniaudio-backed effect nodes.
 *
 * <p>Holds the native node handle and delegates the generic {@link #setParam}/{@link #getParam}
 * contract to the JNI bridge. Typed subclasses extend this and implement the matching
 * {@link FlixelSoundEffect} subinterface, mapping named setters to parameter IDs that
 * correspond to what the C side expects.
 */
class FlixelMiniAudioEffect implements FlixelSoundEffect {

  /** Native effect node handle, or {@code 0} once destroyed. */
  long nodeHandle;

  FlixelMiniAudioEffect(long nodeHandle) {
    this.nodeHandle = nodeHandle;
  }

  /** Returns the native handle so {@link FlixelMiniAudioSound} can wire this node. */
  long getNodeHandle() {
    return nodeHandle;
  }

  @Override
  public void setParam(int paramId, float value) {
    if (nodeHandle != 0L) {
      FlixelMiniAudio.nodeSetParam(nodeHandle, paramId, value);
    }
  }

  @Override
  public float getParam(int paramId) {
    return (nodeHandle != 0L) ? FlixelMiniAudio.nodeGetParam(nodeHandle, paramId) : 0f;
  }

  @Override
  public void destroy() {
    if (nodeHandle != 0L) {
      FlixelMiniAudio.nodeDestroy(nodeHandle);
      nodeHandle = 0L;
    }
  }
}
