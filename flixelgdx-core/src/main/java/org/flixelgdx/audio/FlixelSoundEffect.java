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

import org.flixelgdx.functional.FlixelDestroyable;

/**
 * An opaque handle to an audio-graph effect node (reverb, echo, low-pass, and so on).
 *
 * <p>Effects chain: a sound feeds the first node, each node feeds the next, and the last node
 * feeds the output. Build chains through {@link FlixelSound#addReverb},
 * {@link FlixelSound#addEcho}, {@link FlixelSound#addLowPassMuffle}, and similar; the typed
 * subtypes ({@link FlixelReverbEffect}, {@link FlixelEchoEffect}, {@link FlixelLowPassEffect},
 * {@link FlixelHighPassEffect}, {@link FlixelBandPassEffect}) expose live parameter setters.
 *
 * <p>For custom or registered node types added via {@link FlixelSound#addNode}, the generic
 * {@link #setParam(int, float)} and {@link #getParam(int)} methods address parameters by integer
 * ID without requiring a typed interface.
 *
 * <p>On platforms that do not support a requested node type the returned instance is {@link #NOOP},
 * so effect code is always safe to run regardless of backend.
 */
public interface FlixelSoundEffect extends FlixelDestroyable {

  /** Silent no-op returned when a node type is unsupported on the current backend. */
  FlixelSoundEffect NOOP = new FlixelSoundEffect() {
    public void setParam(int id, float v) {}

    public float getParam(int id) {
      return 0f;
    }

    public void destroy() {}
  };

  /**
   * Sets a node parameter by integer ID.
   *
   * <p>For built-in node types, prefer the named setters on the typed subinterface
   * ({@link FlixelReverbEffect#setWet}, {@link FlixelLowPassEffect#setCutoff}, and so on).
   * This method is the generic path for custom or registered node types.
   *
   * @param paramId Backend-specific parameter index.
   * @param value The new value.
   */
  void setParam(int paramId, float value);

  /**
   * Returns a node parameter by integer ID.
   *
   * <p>Returns {@code 0} when the parameter does not exist or the node is a no-op.
   *
   * @param paramId Backend-specific parameter index.
   * @return The current parameter value.
   */
  float getParam(int paramId);
}
