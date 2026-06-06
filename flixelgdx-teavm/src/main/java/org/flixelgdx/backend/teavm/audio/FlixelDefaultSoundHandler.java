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

import org.flixelgdx.audio.FlixelSoundBackend;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;

/**
 * TeaVM/web implementation of {@link FlixelSoundBackend.Factory} backed by the Web Audio API.
 *
 * <p>A single {@code AudioContext} and master {@code GainNode} are shared across all sounds.
 * Both are created lazily on the first {@link #createSound} call to respect browser policies
 * that require an active user gesture before audio can play.
 *
 * <p>Audio data is read synchronously from the virtual filesystem (which is fully preloaded
 * before the game starts) and decoded asynchronously via {@code AudioContext.decodeAudioData}.
 * This removes the HTML5 {@code <audio>} element limit that restricted concurrent playback to
 * roughly three or four tracks.
 *
 * <p>Focus-based pause and resume are implemented via {@code AudioContext.suspend()} and
 * {@code AudioContext.resume()}, which atomically halts or continues every active sound with no
 * per-instance bookkeeping.
 *
 * <p>Groups are no-ops beyond triggering context-level suspend/resume: the Web Audio API does
 * not expose per-group routing without a dedicated graph, and the engine's SFX and music groups
 * are always paused and resumed together by {@link org.flixelgdx.audio.FlixelAudioManager}.
 */
public class FlixelDefaultSoundHandler implements FlixelSoundBackend.Factory {

  private JSObject context;
  private JSObject masterGainNode;

  private float masterVolume = 1f;

  private boolean contextSuspended;

  /**
   * Initializes the shared {@code AudioContext} and master {@code GainNode} on first use.
   *
   * <p>Deferred to the first {@link #createSound} call so that the context is only created
   * after a user gesture, satisfying browser autoplay policies.
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
    byte[] data = external
        ? Gdx.files.absolute(path).readBytes()
        : Gdx.files.internal(path).readBytes();
    return new FlixelWebAudioSound(path, data, context, masterGainNode);
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
    }
  }

  @Override
  public void attachToEngineOutput(FlixelSoundBackend sound, int outputBusIndex) {
    // No-op on web.
  }

  @Override
  public FlixelSoundBackend.EffectNode createReverbNode(float wet) {
    return NoOpEffectNode.INSTANCE;
  }

  @Override
  public FlixelSoundBackend.EffectNode createDelayNode(float delaySeconds, float decay) {
    return NoOpEffectNode.INSTANCE;
  }

  @Override
  public FlixelSoundBackend.EffectNode createLowPassFilter(double cutoffHz, int order) {
    return NoOpEffectNode.INSTANCE;
  }

  // --- Web Audio API JS bridge ---

  @JSBody(script = "return new (window.AudioContext || window.webkitAudioContext)();")
  private static native JSObject jsCreateContext();

  @JSBody(params = {"ctx"}, script = "return ctx.createGain();")
  private static native JSObject jsCreateGain(JSObject ctx);

  @JSBody(params = {"node", "v"}, script = "node.gain.value = v;")
  private static native void jsSetGain(JSObject node, float v);

  @JSBody(params = {"ctx"}, script = "return ctx.destination;")
  private static native JSObject jsDestination(JSObject ctx);

  @JSBody(params = {"a", "b"}, script = "a.connect(b);")
  private static native void jsConnect(JSObject a, JSObject b);

  @JSBody(params = {"ctx"}, script = "ctx.suspend();")
  private static native void jsSuspend(JSObject ctx);

  @JSBody(params = {"ctx"}, script = "ctx.resume();")
  private static native void jsResume(JSObject ctx);

  @JSBody(params = {"ctx"}, script = "ctx.close();")
  private static native void jsClose(JSObject ctx);

  /** Singleton no-op effect node for platforms that do not support audio graphs. */
  private static final class NoOpEffectNode implements FlixelSoundBackend.EffectNode {

    static final NoOpEffectNode INSTANCE = new NoOpEffectNode();

    @Override
    public void attachToUpstream(FlixelSoundBackend upstream, int bus) {
      // No-op.
    }

    @Override
    public void detach(int bus) {
      // No-op.
    }

    @Override
    public void dispose() {
      // No-op.
    }
  }
}
