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
import org.jetbrains.annotations.NotNull;

/**
 * An opaque handle to an audio-graph effect node (reverb, echo, low-pass, and so on).
 *
 * <p>Effects chain: a sound feeds the first node, each node feeds the next, and the last node
 * feeds the output. Build chains through {@link FlixelSound#addReverb},
 * {@link FlixelSound#addEcho}, and {@link FlixelSound#addLowPassMuffle}; the typed subtypes
 * ({@link FlixelReverbEffect}, {@link FlixelEchoEffect}, {@link FlixelLowPassEffect}) expose
 * live parameter setters.
 *
 * <p>On platforms without an audio graph the returned instances are no-op stubs, so effect
 * code is always safe to run.
 */
public interface FlixelSoundEffect extends FlixelDestroyable {

  /**
   * Wires a sound's output into this node's input.
   *
   * @param upstream The sound whose output feeds this node.
   * @param bus Input bus index (typically 0).
   */
  void attachToUpstreamSound(@NotNull FlixelSound upstream, int bus);

  /**
   * Wires another effect node into this node's input, allowing effects to be chained together
   * (for example, reverb feeding into a low-pass filter).
   *
   * @param upstream The upstream effect node.
   * @param bus Input bus index (typically 0).
   */
  void attachToUpstreamNode(@NotNull FlixelSoundEffect upstream, int bus);

  /**
   * Detaches this node from its input bus.
   *
   * @param bus The bus index to detach.
   */
  void detach(int bus);
}
