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
package org.flixelgdx.backend.html5.audio;

import org.flixelgdx.audio.FlixelSoundEffect;
import org.jetbrains.annotations.NotNull;
import org.teavm.jso.webaudio.AudioNode;

/**
 * Base for all Web Audio API effect nodes.
 *
 * <p>Each subclass wraps one or more Web Audio {@code AudioNode} instances and exposes the node
 * that serves as both the input entry point (receives signal from the upstream source or effect)
 * and the output exit point (sends signal to the downstream effect or final destination). That
 * single node reference is what {@link FlixelWebAudioSound#wireEffectNode} connects into the
 * audio graph.
 *
 * <p>Effects that need to connect additional paths to the output (such as a separate wet/dry gain
 * nodes in reverb) override {@link #onWired} to complete that wiring once the output destination
 * is known.
 */
abstract class FlixelWebAudioEffect implements FlixelSoundEffect {

  @NotNull
  private final AudioNode node;

  FlixelWebAudioEffect(@NotNull AudioNode node) {
    this.node = node;
  }

  /**
   * Returns the Web Audio node that carries this effect's processed signal.
   *
   * <p>{@link FlixelWebAudioSound} uses this to wire the node into the graph during
   * {@link FlixelWebAudioSound#wireEffectNode}.
   */
  @NotNull
  final AudioNode audioNode() {
    return node;
  }

  /**
   * Called by {@link FlixelWebAudioSound#wireEffectNode} immediately after this effect is inserted
   * into the audio graph.
   *
   * <p>The default implementation does nothing. Subclasses that route secondary paths to the same
   * output (such as wet/dry mixing in reverb) override this to complete their internal wiring once
   * the output destination is known.
   *
   * @param output The downstream {@code AudioNode} that this effect's output feeds into.
   */
  void onWired(@NotNull AudioNode output) {}

  @Override
  public void setParam(int paramId, float value) {}

  @Override
  public float getParam(int paramId) {
    return 0f;
  }

  @Override
  public void destroy() {
    node.disconnect();
  }
}
