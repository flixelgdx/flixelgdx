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
 * Web Audio API implementation of {@link FlixelSoundBackend.EchoNode}.
 *
 * <p>Routes the signal through a {@code DelayNode} with a {@code GainNode} feedback path,
 * producing repeating echoes that decay at the rate given by the decay factor. A dry copy of
 * the signal is mixed alongside the wet echo output.
 *
 * <p>Audio graph layout:
 * <pre>
 *   inputNode -+--&gt; delayNode --+--&gt; outputNode --&gt; downstream
 *              |                |
 *              |    feedbackGain &lt;--+
 *              |         |
 *              |         +--&gt; delayNode (feedback loop)
 *              |
 *              +--&gt; outputNode (dry path)
 * </pre>
 */
final class FlixelTeaVMEchoNode implements FlixelSoundBackend.EchoNode, TeaVMAudioNode {

  private final JSObject inputNode;
  private final JSObject delayNode;
  private final JSObject feedbackGain;
  private final JSObject outputNode;

  FlixelTeaVMEchoNode(JSObject context, float delaySeconds, float decay) {
    inputNode = jsCreateGain(context);
    // Max delay headroom: allow up to 5 seconds to accommodate any reasonable initial value.
    delayNode = jsCreateDelay(context, 5.0f);
    feedbackGain = jsCreateGain(context);
    outputNode = jsCreateGain(context);

    jsSetDelayTime(delayNode, Math.max(0f, delaySeconds));
    jsSetGain(feedbackGain, Math.max(0f, Math.min(0.999f, decay)));

    // Dry signal passes straight through.
    jsConnect(inputNode, outputNode);
    // Wet: delay → output and delay → feedback → delay (loop).
    jsConnect(inputNode, delayNode);
    jsConnect(delayNode, outputNode);
    jsConnect(delayNode, feedbackGain);
    jsConnect(feedbackGain, delayNode);
  }

  @Override
  public JSObject getOutputNode() {
    return outputNode;
  }

  @Override
  public void attachToUpstream(FlixelSoundBackend upstream, int bus) {
    if (upstream instanceof TeaVMAudioNode n) {
      jsDisconnect(n.getOutputNode());
      jsConnect(n.getOutputNode(), inputNode);
    }
  }

  @Override
  public void attachToUpstreamNode(FlixelSoundBackend.EffectNode upstream, int bus) {
    if (upstream instanceof TeaVMAudioNode n) {
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
    jsDisconnect(feedbackGain);
    jsDisconnect(delayNode);
    jsDisconnect(inputNode);
  }

  @JSBody(params = { "ctx" }, script = "return ctx.createGain();")
  private static native JSObject jsCreateGain(JSObject ctx);

  @JSBody(params = { "ctx", "maxDelay" }, script = "return ctx.createDelay(maxDelay);")
  private static native JSObject jsCreateDelay(JSObject ctx, float maxDelay);

  @JSBody(params = { "a", "b" }, script = "a.connect(b);")
  private static native void jsConnect(JSObject a, JSObject b);

  @JSBody(params = { "node" }, script = "try { node.disconnect(); } catch(e) {}")
  private static native void jsDisconnect(JSObject node);

  @JSBody(params = { "node", "v" }, script = "node.gain.value = v;")
  private static native void jsSetGain(JSObject node, float v);

  @JSBody(params = { "delayNode", "seconds" }, script = "delayNode.delayTime.value = seconds;")
  private static native void jsSetDelayTime(JSObject delayNode, float seconds);
}
