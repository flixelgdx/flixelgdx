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
package org.flixelgdx.backend.teavm.audio;

import org.flixelgdx.audio.FlixelSoundBackend;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;

/**
 * Web Audio API implementation of {@link FlixelSoundBackend.ReverbNode}.
 *
 * <p>Uses a {@code ConvolverNode} driven by a procedurally generated impulse response: two
 * channels of exponentially-decaying white noise whose length is proportional to room size and
 * whose decay rate is controlled by the damping parameter. The stereo spread of each channel's
 * noise is blended by the width parameter.
 *
 * <p>Wet and dry levels are controlled by dedicated {@code GainNode}s and take effect
 * immediately. Room size, damping, and width regenerate the impulse response on each call,
 * which causes a brief audible transition in the reverb tail. Apply these during loading or
 * state transitions, not continuously per frame.
 *
 * <p>The frozen mode approximates the MiniAudio behavior by pinning room size to maximum and
 * setting damping to zero so the tail decays as slowly as possible.
 *
 * <p>Audio graph layout:
 * <pre>
 *   inputNode -+--> convolverNode ----> wetGain ----+---> outputNode --> downstream
 *              |                                    |
 *              +--> dryGain ------------------------+
 * </pre>
 */
final class FlixelTeaVMReverbNode implements FlixelSoundBackend.ReverbNode, FlixelTeaVMAudioNode {

  private final JSObject context;
  private final JSObject inputNode;
  private final JSObject convolverNode;
  private final JSObject wetGain;
  private final JSObject dryGain;
  private final JSObject outputNode;

  private float wet;
  private float dry;
  private float roomSize = 0.7f;
  private float damping = 0.5f;
  private float width = 1.0f;
  private boolean frozen;

  FlixelTeaVMReverbNode(JSObject context, float wet) {
    this.context = context;
    inputNode = jsCreateGain(context);
    convolverNode = jsCreateConvolver(context);
    wetGain = jsCreateGain(context);
    dryGain = jsCreateGain(context);
    outputNode = jsCreateGain(context);

    jsConnect(inputNode, convolverNode);
    jsConnect(inputNode, dryGain);
    jsConnect(convolverNode, wetGain);
    jsConnect(wetGain, outputNode);
    jsConnect(dryGain, outputNode);

    this.wet = Math.max(0f, Math.min(1f, wet));
    this.dry = 1f - this.wet;
    jsSetGain(wetGain, this.wet);
    jsSetGain(dryGain, this.dry);

    jsSetConvolverBuffer(convolverNode, jsGenerateIR(context, roomSize, damping, width));
  }

  @Override
  public JSObject getOutputNode() {
    return outputNode;
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
    this.wet = Math.max(0f, Math.min(1f, wet));
    jsSetGain(wetGain, this.wet);
  }

  @Override
  public void setDry(float dry) {
    this.dry = Math.max(0f, Math.min(1f, dry));
    jsSetGain(dryGain, this.dry);
  }

  @Override
  public void setRoomSize(float size) {
    roomSize = Math.max(0f, Math.min(1f, size));
    refreshIR();
  }

  @Override
  public void setDamping(float damping) {
    this.damping = Math.max(0f, Math.min(1f, damping));
    refreshIR();
  }

  @Override
  public void setWidth(float width) {
    this.width = Math.max(0f, Math.min(1f, width));
    refreshIR();
  }

  @Override
  public void setFrozen(boolean frozen) {
    this.frozen = frozen;
    refreshIR();
  }

  private void refreshIR() {
    float rs = frozen ? 1.0f : roomSize;
    float d = frozen ? 0.0f : damping;
    jsSetConvolverBuffer(convolverNode, jsGenerateIR(context, rs, d, width));
  }

  @Override
  public void attachToUpstream(FlixelSoundBackend upstream, int bus) {
    if (upstream instanceof FlixelTeaVMAudioNode n) {
      jsDisconnect(n.getOutputNode());
      jsConnect(n.getOutputNode(), inputNode);
    }
  }

  @Override
  public void attachToUpstreamNode(FlixelSoundBackend.EffectNode upstream, int bus) {
    if (upstream instanceof FlixelTeaVMAudioNode n) {
      jsDisconnect(n.getOutputNode());
      jsConnect(n.getOutputNode(), inputNode);
    }
  }

  @Override
  public void detach(int bus) {
    // Intentional no-op: dispose() handles full disconnection.
  }

  @Override
  public void dispose() {
    jsDisconnect(outputNode);
    jsDisconnect(convolverNode);
    jsDisconnect(dryGain);
    jsDisconnect(wetGain);
    jsDisconnect(inputNode);
  }

  @JSBody(params = { "ctx" }, script = "return ctx.createGain();")
  private static native JSObject jsCreateGain(JSObject ctx);

  @JSBody(params = { "ctx" }, script = "return ctx.createConvolver();")
  private static native JSObject jsCreateConvolver(JSObject ctx);

  @JSBody(params = { "a", "b" }, script = "a.connect(b);")
  private static native void jsConnect(JSObject a, JSObject b);

  @JSBody(params = { "node" }, script = "try { node.disconnect(); } catch(e) {}")
  private static native void jsDisconnect(JSObject node);

  @JSBody(params = { "node", "v" }, script = "node.gain.value = v;")
  private static native void jsSetGain(JSObject node, float v);

  @JSBody(params = { "conv", "buf" }, script = "conv.buffer = buf;")
  private static native void jsSetConvolverBuffer(JSObject conv, JSObject buf);

  /**
   * Generates a stereo impulse response as two channels of exponentially-decaying white noise.
   *
   * <p>The duration (0.2 s to 4 s) scales with {@code roomSize}. The decay envelope exponent
   * scales with {@code damping}: higher values cause the tail to fade faster. The {@code width}
   * parameter blends from mono (0) to fully independent stereo channels (1).
   */
  @JSBody(
      params = { "ctx", "roomSize", "damping", "width" },
      script = "var sampleRate = ctx.sampleRate;"
          + "var duration = 0.2 + roomSize * 3.8;"
          + "var length = Math.ceil(sampleRate * duration);"
          + "var ir = ctx.createBuffer(2, length, sampleRate);"
          + "var dataL = ir.getChannelData(0);"
          + "var dataR = ir.getChannelData(1);"
          + "var decayBase = Math.max(0.001, 1.0 - damping);"
          + "for (var i = 0; i < length; i++) {"
          + "  var t = i / length;"
          + "  var env = Math.pow(decayBase, t * 10);"
          + "  var nL = (Math.random() * 2 - 1) * env;"
          + "  var nR = (Math.random() * 2 - 1) * env;"
          + "  var mono = (nL + nR) * 0.5;"
          + "  dataL[i] = mono + (nL - mono) * width;"
          + "  dataR[i] = mono + (nR - mono) * width;"
          + "}"
          + "return ir;")
  private static native JSObject jsGenerateIR(JSObject ctx, float roomSize, float damping,
      float width);
}
