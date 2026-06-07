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
 * Web Audio API implementation of {@link FlixelSoundBackend.LowPassNode}.
 *
 * <p>Wraps a {@code BiquadFilterNode} of type {@code "lowpass"}. The cutoff frequency is an
 * {@code AudioParam} and can be updated at any time via {@link #setCutoff(double)} without
 * interrupting playback.
 *
 * <p>Audio graph layout:
 * <pre>
 *   upstream --&gt; filterNode --&gt; downstream
 * </pre>
 */
final class FlixelTeaVMLowPassNode implements FlixelSoundBackend.LowPassNode, FlixelTeaVMAudioNode {

  private double cutoffHz;
  private final JSObject filterNode;

  FlixelTeaVMLowPassNode(JSObject context, double cutoffHz) {
    this.cutoffHz = cutoffHz;
    filterNode = jsCreateLowPassFilter(context);
    jsSetCutoff(filterNode, cutoffHz);
  }

  @Override
  public JSObject getOutputNode() {
    return filterNode;
  }

  @Override
  public double getCutoff() {
    return cutoffHz;
  }

  @Override
  public void setCutoff(double hz) {
    cutoffHz = hz;
    jsSetCutoff(filterNode, hz);
  }

  @Override
  public void attachToUpstream(FlixelSoundBackend upstream, int bus) {
    if (upstream instanceof FlixelTeaVMAudioNode n) {
      jsDisconnect(n.getOutputNode());
      jsConnect(n.getOutputNode(), filterNode);
    }
  }

  @Override
  public void attachToUpstreamNode(FlixelSoundBackend.EffectNode upstream, int bus) {
    if (upstream instanceof FlixelTeaVMAudioNode n) {
      jsDisconnect(n.getOutputNode());
      jsConnect(n.getOutputNode(), filterNode);
    }
  }

  @Override
  public void detach(int bus) {
    // Intentional no-op: dispose() handles full disconnection.
  }

  @Override
  public void dispose() {
    jsDisconnect(filterNode);
  }

  @JSBody(
      params = { "ctx" },
      script = "var f = ctx.createBiquadFilter(); f.type = 'lowpass'; return f;")
  private static native JSObject jsCreateLowPassFilter(JSObject ctx);

  @JSBody(params = { "a", "b" }, script = "a.connect(b);")
  private static native void jsConnect(JSObject a, JSObject b);

  @JSBody(params = { "node" }, script = "try { node.disconnect(); } catch(e) {}")
  private static native void jsDisconnect(JSObject node);

  @JSBody(params = { "filter", "hz" }, script = "filter.frequency.value = hz;")
  private static native void jsSetCutoff(JSObject filter, double hz);
}
