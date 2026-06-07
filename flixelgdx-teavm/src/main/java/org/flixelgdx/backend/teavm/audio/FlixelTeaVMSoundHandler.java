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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.ObjectMap;
import com.github.xpenatan.gdx.teavm.backends.web.dom.typedarray.TypedArrays;

import org.flixelgdx.audio.FlixelSoundBackend;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.jso.typedarrays.ArrayBuffer;

/**
 * TeaVM/web implementation of {@link FlixelSoundBackend.Factory} backed by the Web Audio API.
 *
 * <p>A single {@code AudioContext} and master {@code GainNode} are shared across all sounds.
 * Both are created lazily on the first {@link #createSound} or {@link #prewarmSound} call to
 * respect browser policies that require an active user gesture before audio can play.
 *
 * <p>Audio data is read synchronously from the virtual filesystem (which is fully preloaded
 * before the game starts) and decoded asynchronously via {@code AudioContext.decodeAudioData}.
 *
 * <p>Decoded {@code AudioBuffer} objects are cached by path. Once a path has been decoded
 * (either by a previous {@link #createSound} call or by an explicit {@link #prewarmSound}
 * call), subsequent {@link #createSound} calls for that path use the cached buffer and start
 * playback immediately with no decode lag. This is the mechanism that keeps multiple tracks
 * started in the same frame in perfect sync without skipping into the audio.
 *
 * <p>Focus-based pause and resume are implemented via {@code AudioContext.suspend()} and
 * {@code AudioContext.resume()}, which atomically halts or continues every active sound with no
 * per-instance bookkeeping.
 *
 * <p>Groups are no-ops beyond triggering context-level suspend/resume: the Web Audio API does
 * not expose per-group routing without a dedicated graph, and the engine's SFX and music groups
 * are always paused and resumed together by {@link org.flixelgdx.audio.FlixelAudioManager}.
 *
 * <p>Audio-graph effects ({@link org.flixelgdx.audio.FlixelSoundBackend.ReverbNode},
 * {@link org.flixelgdx.audio.FlixelSoundBackend.EchoNode},
 * {@link org.flixelgdx.audio.FlixelSoundBackend.LowPassNode}) are fully supported on web
 * using native Web Audio API nodes. Reverb uses a {@code ConvolverNode} driven by a
 * procedurally generated impulse response; echo uses a {@code DelayNode} with a feedback
 * {@code GainNode}; low-pass uses a {@code BiquadFilterNode}.
 */
public class FlixelTeaVMSoundHandler implements FlixelSoundBackend.Factory {

  private JSObject context;
  private JSObject masterGainNode;
  private final ObjectMap<String, JSObject> bufferCache = new ObjectMap<>();
  private final ObjectMap<String, Double> lengthCache = new ObjectMap<>();

  private float masterVolume = 1f;
  private int pendingPrewarmCount;

  private boolean contextSuspended;

  /**
   * Initializes the shared {@code AudioContext} and master {@code GainNode} on first use.
   *
   * <p>Deferred to the first {@link #createSound} or {@link #prewarmSound} call so that the
   * context is only created after a user gesture, satisfying browser autoplay policies.
   */
  private void ensureContext() {
    if (context != null) {
      return;
    }
    context = jsCreateContext();
    masterGainNode = jsCreateGain(context);
    jsConnect(masterGainNode, jsDestination(context));
    jsSetGain(masterGainNode, masterVolume);
  }

  @Override
  public FlixelSoundBackend createSound(String path, short flags, Object group, boolean external) {
    ensureContext();
    if (!external && bufferCache.containsKey(path)) {
      JSObject buf = bufferCache.get(path);
      double len = lengthCache.get(path);
      return new FlixelTeaVMSound(path, buf, len, context, masterGainNode);
    }
    byte[] data = external
        ? Gdx.files.absolute(path).readBytes()
        : Gdx.files.internal(path).readBytes();
    return new FlixelTeaVMSound(path, data, context, masterGainNode,
        external ? null : (buf, len) -> {
          bufferCache.put(path, buf);
          lengthCache.put(path, len);
        });
  }

  @Override
  public void prewarmSound(String path) {
    ensureContext();
    if (bufferCache.containsKey(path)) {
      return;
    }
    pendingPrewarmCount++;
    byte[] data = Gdx.files.internal(path).readBytes();
    ArrayBuffer buffer = TypedArrays.getInt8Array(data).getBuffer();
    jsDecodeAudioData(context, buffer,
        decoded -> {
          bufferCache.put(path, decoded);
          lengthCache.put(path, jsGetBufferDuration(decoded));
          pendingPrewarmCount--;
        },
        () -> {
          System.err.println("[FlixelGDX] Failed to pre-decode audio: " + path);
          pendingPrewarmCount--;
        });
  }

  @Override
  public boolean isPrewarmPending() {
    return pendingPrewarmCount > 0;
  }

  @Override
  public Object createGroup() {
    return new Object();
  }

  @Override
  public void disposeGroup(Object group) {
    // No-op on web.
  }

  @Override
  public void groupPause(Object group) {
    if (context != null && !contextSuspended) {
      contextSuspended = true;
      jsSuspend(context);
    }
  }

  @Override
  public void groupPlay(Object group) {
    if (context != null && contextSuspended) {
      contextSuspended = false;
      jsResume(context);
    }
  }

  @Override
  public void setMasterVolume(float volume) {
    masterVolume = Math.max(0f, Math.min(1f, volume));
    if (masterGainNode != null) {
      jsSetGain(masterGainNode, masterVolume);
    }
  }

  /**
   * Returns the tracked master volume.
   *
   * @return Master volume in [0, 1].
   */
  public float getMasterVolume() {
    return masterVolume;
  }

  @Override
  public void disposeEngine() {
    if (context != null) {
      jsClose(context);
      context = null;
      masterGainNode = null;
      bufferCache.clear();
      lengthCache.clear();
    }
  }

  @Override
  public void attachToEngineOutput(FlixelSoundBackend sound, int outputBusIndex) {
    if (masterGainNode == null || !(sound instanceof TeaVMAudioNode n)) {
      return;
    }
    jsDisconnect(n.getOutputNode());
    jsConnect(n.getOutputNode(), masterGainNode);
  }

  @Override
  public void attachEffectToEngineOutput(FlixelSoundBackend.EffectNode node, int outputBusIndex) {
    if (masterGainNode == null || !(node instanceof TeaVMAudioNode n)) {
      return;
    }
    jsDisconnect(n.getOutputNode());
    jsConnect(n.getOutputNode(), masterGainNode);
  }

  @Override
  public FlixelSoundBackend.ReverbNode createReverbNode(float wet) {
    ensureContext();
    return new FlixelTeaVMReverbNode(context, wet);
  }

  @Override
  public FlixelSoundBackend.EchoNode createDelayNode(float delaySeconds, float decay) {
    ensureContext();
    return new FlixelTeaVMEchoNode(context, delaySeconds, decay);
  }

  @Override
  public FlixelSoundBackend.LowPassNode createLowPassFilter(double cutoffHz, int order) {
    ensureContext();
    return new FlixelTeaVMLowPassNode(context, cutoffHz);
  }

  @JSBody(script = "return new (window.AudioContext || window.webkitAudioContext)();")
  private static native JSObject jsCreateContext();

  @JSBody(params = {"node"}, script = "try { node.disconnect(); } catch(e) {}")
  private static native void jsDisconnect(JSObject node);

  @JSBody(params = { "ctx" }, script = "return ctx.createGain();")
  private static native JSObject jsCreateGain(JSObject ctx);

  @JSBody(params = { "node", "v" }, script = "node.gain.value = v;")
  private static native void jsSetGain(JSObject node, float v);

  @JSBody(params = { "ctx" }, script = "return ctx.destination;")
  private static native JSObject jsDestination(JSObject ctx);

  @JSBody(params = { "a", "b" }, script = "a.connect(b);")
  private static native void jsConnect(JSObject a, JSObject b);

  @JSBody(params = { "ctx" }, script = "ctx.suspend();")
  private static native void jsSuspend(JSObject ctx);

  @JSBody(params = { "ctx" }, script = "ctx.resume();")
  private static native void jsResume(JSObject ctx);

  @JSBody(params = { "ctx" }, script = "ctx.close();")
  private static native void jsClose(JSObject ctx);

  @JSBody(params = { "ctx", "buf", "onOk", "onErr" },
      script = "ctx.decodeAudioData(buf, onOk, onErr);")
  private static native void jsDecodeAudioData(JSObject ctx, ArrayBuffer buf,
      PrewarmSuccessHandler onOk, PrewarmErrorHandler onErr);

  @JSBody(params = { "buf" }, script = "return buf.duration;")
  private static native double jsGetBufferDuration(JSObject buf);

  /** JS-callable success callback used by {@link #prewarmSound}. */
  @JSFunctor
  @FunctionalInterface
  private interface PrewarmSuccessHandler extends JSObject {
    void onDecoded(JSObject audioBuffer);
  }

  /** JS-callable error callback used by {@link #prewarmSound}. */
  @JSFunctor
  @FunctionalInterface
  private interface PrewarmErrorHandler extends JSObject {
    void onError();
  }
}
