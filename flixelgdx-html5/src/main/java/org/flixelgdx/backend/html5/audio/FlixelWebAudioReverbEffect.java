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

import org.flixelgdx.audio.FlixelReverbEffect;
import org.jetbrains.annotations.NotNull;
import org.teavm.jso.webaudio.AudioContext;
import org.teavm.jso.webaudio.AudioNode;
import org.teavm.jso.webaudio.ConvolverNode;
import org.teavm.jso.webaudio.GainNode;

/**
 * Web Audio reverb effect built from a {@code ConvolverNode} with a synthetically generated
 * impulse response and a wet/dry mixing stage.
 *
 * <p>The impulse response is exponentially-decaying white noise whose tail length is set by
 * {@code roomSize} and whose high-frequency absorption is approximated by rolling off the
 * amplitude linearly with {@code damping}. A stereo decorrelation is applied using mid-side
 * encoding scaled by {@code width}: at width 0 the two channels are identical; at width 1 they
 * are fully independent noise sequences.
 *
 * <p>The audio graph inside this effect is:
 * <pre>
 *   [inputGain] --dry--> [dryGain] -----> [output]
 *               --wet--> [convolver] -> [wetGain] -> [output]
 * </pre>
 *
 * <p>The top connection ({@code inputGain -> output}) is created by
 * {@link FlixelWebAudioSound#wireEffectNode}. {@link #onWired} is then called to redirect it
 * through {@code dryGain} and add the parallel wet path.
 *
 * <p>Changing {@code roomSize}, {@code damping}, {@code width}, or {@code frozen} regenerates the
 * impulse response buffer and resets the convolver, which is a brief but audible discontinuity.
 */
class FlixelWebAudioReverbEffect extends FlixelWebAudioEffect implements FlixelReverbEffect {

  @NotNull
  private final AudioContext context;

  @NotNull
  private final ConvolverNode convolver;

  @NotNull
  private final GainNode wetGain;

  @NotNull
  private final GainNode dryGain;

  private float wet;
  private float dry;
  private float roomSize;
  private float damping;
  private float width;
  private boolean frozen;

  private FlixelWebAudioReverbEffect(@NotNull AudioContext context, @NotNull GainNode inputGain,
      @NotNull ConvolverNode convolver, @NotNull GainNode wetGain, @NotNull GainNode dryGain,
      float wet, float dry, float roomSize, float damping, float width) {
    super(inputGain);
    this.context = context;
    this.convolver = convolver;
    this.wetGain = wetGain;
    this.dryGain = dryGain;
    this.wet = wet;
    this.dry = dry;
    this.roomSize = roomSize;
    this.damping = damping;
    this.width = width;
    FlixelWebAudioFactory.connect(inputGain, convolver);
    FlixelWebAudioFactory.connect(convolver, wetGain);
    wetGain.getGain().setValue(wet);
    dryGain.getGain().setValue(dry);
    convolver.setBuffer(buildIr(context, roomSize, damping, width, false));
  }

  /**
   * Creates a reverb effect.
   *
   * @param context The shared audio context used to allocate nodes and the impulse response.
   * @param wet Initial wet (processed) level.
   * @param dry Initial dry (direct) level.
   * @param roomSize Simulated room size in {@code [0, 1]}; larger values produce longer tails.
   * @param damping High-frequency absorption in {@code [0, 1]}.
   * @param width Stereo width in {@code [0, 1]}; 0 collapses both channels to mono.
   * @return A configured reverb effect ready to be inserted into a sound's effect chain.
   */
  @NotNull
  static FlixelWebAudioReverbEffect create(@NotNull AudioContext context, float wet, float dry,
      float roomSize, float damping, float width) {
    GainNode inputGain = context.createGain();
    ConvolverNode convolver = context.createConvolver();
    GainNode wetGain = context.createGain();
    GainNode dryGain = context.createGain();
    return new FlixelWebAudioReverbEffect(
        context, inputGain, convolver, wetGain, dryGain, wet, dry, roomSize, damping, width);
  }

  @Override
  void onWired(@NotNull AudioNode output) {
    // wireEffectNode connected inputGain directly to output as the "effect exit." Redirect that
    // through dryGain so the dry signal is level-controlled, then add the wet path alongside it.
    audioNode().disconnect(output);
    FlixelWebAudioFactory.connect(audioNode(), dryGain);
    FlixelWebAudioFactory.connect(dryGain, output);
    FlixelWebAudioFactory.connect(wetGain, output);
  }

  @Override
  public float getWet() {
    return wet;
  }

  @Override
  public float getDry() {
    return dry;
  }

  @Override
  public float getRoomSize() {
    return roomSize;
  }

  @Override
  public float getDamping() {
    return damping;
  }

  @Override
  public float getWidth() {
    return width;
  }

  @Override
  public boolean isFrozen() {
    return frozen;
  }

  @Override
  public void setWet(float wet) {
    this.wet = wet;
    wetGain.getGain().setValue(wet);
  }

  @Override
  public void setDry(float dry) {
    this.dry = dry;
    dryGain.getGain().setValue(dry);
  }

  @Override
  public void setRoomSize(float size) {
    roomSize = size;
    convolver.setBuffer(buildIr(context, roomSize, damping, width, frozen));
  }

  @Override
  public void setDamping(float damping) {
    this.damping = damping;
    convolver.setBuffer(buildIr(context, roomSize, damping, width, frozen));
  }

  @Override
  public void setWidth(float width) {
    this.width = width;
    convolver.setBuffer(buildIr(context, roomSize, damping, width, frozen));
  }

  @Override
  public void setFrozen(boolean frozen) {
    this.frozen = frozen;
    convolver.setBuffer(buildIr(context, roomSize, damping, width, frozen));
  }

  @Override
  public void setParam(int paramId, float value) {
    if (paramId == 0) {
      setWet(value);
    } else if (paramId == 1) {
      setDry(value);
    } else if (paramId == 2) {
      setRoomSize(value);
    } else if (paramId == 3) {
      setDamping(value);
    } else if (paramId == 4) {
      setWidth(value);
    } else if (paramId == 5) {
      setFrozen(value != 0f);
    }
  }

  @Override
  public float getParam(int paramId) {
    if (paramId == 0) {
      return wet;
    }
    if (paramId == 1) {
      return dry;
    }
    if (paramId == 2) {
      return roomSize;
    }
    if (paramId == 3) {
      return damping;
    }
    if (paramId == 4) {
      return width;
    }
    if (paramId == 5) {
      return frozen ? 1f : 0f;
    }
    return 0f;
  }

  @Override
  public void destroy() {
    dryGain.disconnect();
    convolver.disconnect();
    wetGain.disconnect();
    super.destroy();
  }

  /**
   * Generates a stereo impulse response whose tail shape is controlled by {@code roomSize},
   * {@code damping}, and {@code width}.
   *
   * <p>The IR is exponentially decaying white noise. The decay rate shortens as {@code roomSize}
   * increases. {@code damping} applies a linear amplitude roll-off toward the tail end, simulating
   * high-frequency absorption. Stereo width is achieved via mid-side encoding: the two channels
   * share a common mid signal plus a side component scaled by {@code width}.
   *
   * <p>When {@code frozen} is {@code true} the decay envelope is replaced by a flat one, causing
   * the reverb tail to sustain indefinitely.
   */
  private static org.teavm.jso.webaudio.AudioBuffer buildIr(@NotNull AudioContext context,
      float roomSize, float damping, float width, boolean frozen) {
    float sampleRate = context.getSampleRate();
    int length = (int) (sampleRate * Math.max(0.1f, roomSize * 3.0f));
    org.teavm.jso.webaudio.AudioBuffer buffer = context.createBuffer(2, length, sampleRate);
    float[] left = new float[length];
    float[] right = new float[length];
    double decayRate = frozen ? 0.0 : Math.log(0.001) / length * (2.0 - roomSize);
    for (int i = 0; i < length; i++) {
      double t = (double) i / length;
      double envelope = frozen ? 0.5 : Math.exp(decayRate * i) * (1.0 - damping * t);
      double mid = (Math.random() * 2.0 - 1.0) * envelope;
      double side = (Math.random() * 2.0 - 1.0) * envelope * width;
      left[i] = (float) (mid + side);
      right[i] = (float) (mid - side);
    }
    buffer.copyToChannel(left, 0);
    buffer.copyToChannel(right, 1);
    return buffer;
  }
}
