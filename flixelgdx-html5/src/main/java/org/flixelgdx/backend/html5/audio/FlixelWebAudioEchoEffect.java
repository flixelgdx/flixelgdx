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

import org.flixelgdx.audio.FlixelEchoEffect;
import org.jetbrains.annotations.NotNull;
import org.teavm.jso.webaudio.AudioContext;
import org.teavm.jso.webaudio.DelayNode;
import org.teavm.jso.webaudio.GainNode;

/**
 * Web Audio echo effect built from a {@code DelayNode} plus a feedback {@code GainNode}.
 *
 * <p>The feedback loop is: delay output feeds into a gain node whose output reconnects back into
 * the delay node's input. This causes each repeat to be quieter than the previous one by the
 * {@code decay} factor, producing the classic echo tail. Use {@link #create} to instantiate since
 * constructing the feedback cycle requires two nodes before {@code super()} can be called.
 *
 * <p>Connections managed by this class (beyond those wired by
 * {@link FlixelWebAudioSound#wireEffectNode}):
 * <ul>
 *   <li>{@code delayNode -> feedbackGain} - taps the delay output into the feedback path</li>
 *   <li>{@code feedbackGain -> delayNode} - feeds the attenuated signal back into the delay</li>
 * </ul>
 */
class FlixelWebAudioEchoEffect extends FlixelWebAudioEffect implements FlixelEchoEffect {

  @NotNull
  private final GainNode feedbackGain;

  private float decay;
  private float delay;

  private FlixelWebAudioEchoEffect(@NotNull DelayNode delayNode, @NotNull GainNode feedbackGain,
      float delaySeconds, float decay) {
    super(delayNode);
    this.feedbackGain = feedbackGain;
    this.delay = delaySeconds;
    this.decay = decay;
    delayNode.connect(feedbackGain);
    feedbackGain.connect(delayNode);
  }

  /**
   * Creates an echo effect with the given delay time and feedback amount.
   *
   * @param context The shared audio context used to allocate the nodes.
   * @param delaySeconds How long (in seconds) before the first repeat is heard.
   * @param decay Amplitude multiplier for each successive repeat (0 = no echo, 1 = infinite).
   * @return A configured echo effect ready to be wired into a sound's effect chain.
   */
  @NotNull
  static FlixelWebAudioEchoEffect create(@NotNull AudioContext context, float delaySeconds,
      float decay) {
    DelayNode delayNode = context.createDelay(Math.max(delaySeconds * 2.0, 5.0));
    GainNode feedbackGain = context.createGain();
    delayNode.getDelayTime().setValue(delaySeconds);
    feedbackGain.getGain().setValue(decay);
    return new FlixelWebAudioEchoEffect(delayNode, feedbackGain, delaySeconds, decay);
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
  public void setDelay(float delaySeconds) {
    delay = delaySeconds;
    delayNode().getDelayTime().setValue(delaySeconds);
  }

  @Override
  public void setDecay(float decayFactor) {
    decay = decayFactor;
    feedbackGain.getGain().setValue(decayFactor);
  }

  @Override
  public void setParam(int paramId, float value) {
    if (paramId == 0) {
      setDecay(value);
    } else if (paramId == 1) {
      setDelay(value);
    }
  }

  @Override
  public float getParam(int paramId) {
    if (paramId == 0) {
      return decay;
    }
    if (paramId == 1) {
      return delay;
    }
    return 0f;
  }

  @Override
  public void destroy() {
    feedbackGain.disconnect();
    super.destroy();
  }

  private DelayNode delayNode() {
    return (DelayNode) audioNode();
  }
}
