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

import com.github.xpenatan.gdx.teavm.backends.web.dom.typedarray.TypedArrays;

import org.flixelgdx.audio.FlixelSoundBackend;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.jso.typedarrays.ArrayBuffer;

/**
 * TeaVM/web implementation of {@link FlixelSoundBackend} backed directly by the Web Audio API.
 *
 * <p>Each instance holds a decoded {@code AudioBuffer} and routes audio through a
 * persistent {@code GainNode} (per-sound volume) and {@code StereoPannerNode} (stereo pan),
 * both of which connect to a shared master {@code GainNode} owned by the factory. A fresh
 * {@code AudioBufferSourceNode} is created on every {@link #play()} call because source
 * nodes are single-use and cannot be restarted.
 *
 * <p>Decoding is asynchronous: the raw file bytes are passed to
 * {@code AudioContext.decodeAudioData} on construction. If {@link #play()} is called before
 * decoding finishes, the call is queued and executed automatically when the buffer is ready.
 *
 * <p>Position is tracked by recording {@code AudioContext.currentTime} at start and
 * computing the elapsed offset each frame, so no polling timer is needed.
 *
 * <p>Example usage (indirect, via {@link org.flixelgdx.audio.FlixelAudioManager}):
 *
 * <pre>{@code
 * Flixel.sound.play("music/song.mp3");
 * }</pre>
 */
final class FlixelTeaVMSound implements FlixelSoundBackend, TeaVMAudioNode {

  private double startContextTime;
  private double pauseOffset;
  private double totalLength;
  // Context time recorded when play() is called before decode completes. Used to
  // compensate for decode lag so tracks started together stay in sync.
  private double pendingStartContextTime = -1.0;

  private final JSObject context;
  private final JSObject gainNode;
  private final JSObject pannerNode;
  private JSObject audioBuffer;
  private JSObject sourceNode;
  private final String path;

  private float volume = 1f;
  private float pitch = 1f;

  private boolean looping;
  private boolean playing;
  private boolean ended;
  private boolean decoded;
  private boolean pendingPlay;
  private boolean manuallyStopped;

  /**
   * JS-callable success callback fired when {@code AudioContext.decodeAudioData} finishes.
   *
   * @see JSFunctor
   */
  @JSFunctor
  @FunctionalInterface
  interface DecodeSuccessHandler extends JSObject {
    void onDecoded(JSObject audioBuffer);
  }

  /**
   * JS-callable error callback fired when {@code AudioContext.decodeAudioData} fails.
   *
   * @see JSFunctor
   */
  @JSFunctor
  @FunctionalInterface
  interface DecodeErrorHandler extends JSObject {
    void onError();
  }

  /**
   * JS-callable callback set on {@code AudioBufferSourceNode.onended}.
   *
   * @see JSFunctor
   */
  @JSFunctor
  @FunctionalInterface
  interface EndedHandler extends JSObject {
    void onEnded();
  }

  /**
   * Creates a new sound from a pre-decoded {@code AudioBuffer} obtained from the factory cache.
   *
   * <p>Because the buffer is already decoded, this constructor never blocks and the
   * sound is ready to play immediately. Use {@link FlixelTeaVMSoundHandler#prewarmSound}
   * (via {@link org.flixelgdx.audio.FlixelAudioManager#prewarmSound}) to populate the
   * cache before calling play.
   *
   * @param path The asset path, used only for error messages.
   * @param audioBuffer A decoded {@code AudioBuffer} returned by {@code decodeAudioData}.
   * @param length Duration of the audio in seconds.
   * @param context The shared {@code AudioContext} created by the factory.
   * @param masterGainNode The factory-owned master gain node to connect into.
   */
  FlixelTeaVMSound(String path, JSObject audioBuffer, double length,
      JSObject context, JSObject masterGainNode) {
    this.path = path;
    this.context = context;
    this.gainNode = jsCreateGain(context);
    this.pannerNode = jsCreateStereoPanner(context);
    jsConnect(gainNode, pannerNode);
    jsConnect(pannerNode, masterGainNode);
    this.audioBuffer = audioBuffer;
    this.totalLength = length;
    this.decoded = true;
  }

  /**
   * Creates a new sound and begins async decoding of the provided audio data.
   *
   * @param path The asset path, used only for error messages.
   * @param data Raw audio file bytes (e.g. MP3, OGG) from the virtual filesystem.
   * @param context The shared {@code AudioContext} created by the factory.
   * @param masterGainNode The factory-owned master gain node to connect into.
   */
  FlixelTeaVMSound(String path, byte[] data, JSObject context, JSObject masterGainNode) {
    this(path, data, context, masterGainNode, null);
  }

  /**
   * Creates a new sound, begins async decoding, and notifies {@code onDecoded} when the
   * buffer is ready. The factory uses this callback to populate its cache so subsequent
   * calls for the same path skip decoding entirely.
   *
   * @param path The asset path, used only for error messages.
   * @param data Raw audio file bytes (e.g. MP3, OGG) from the virtual filesystem.
   * @param context The shared {@code AudioContext} created by the factory.
   * @param masterGainNode The factory-owned master gain node to connect into.
   * @param onDecoded Called once with the decoded buffer when decoding succeeds,
   *                  or {@code null} if no notification is needed.
   */
  FlixelTeaVMSound(String path, byte[] data, JSObject context, JSObject masterGainNode,
      AudioBufferCallback onDecoded) {
    this.path = path;
    this.context = context;
    this.gainNode = jsCreateGain(context);
    this.pannerNode = jsCreateStereoPanner(context);
    jsConnect(gainNode, pannerNode);
    jsConnect(pannerNode, masterGainNode);

    ArrayBuffer buffer = TypedArrays.getInt8Array(data).getBuffer();
    jsDecodeAudioData(context, buffer,
        decoded -> {
          this.audioBuffer = decoded;
          this.totalLength = jsGetBufferDuration(decoded);
          this.decoded = true;
          if (onDecoded != null) {
            onDecoded.onDecoded(decoded, this.totalLength);
          }
          if (pendingPlay) {
            pendingPlay = false;
            playInternal();
          }
        },
        () -> System.err.println("[FlixelGDX] Failed to decode audio: " + path));
  }

  @Override
  public void play() {
    if (playing) {
      return;
    }
    ended = false;
    if (!decoded) {
      pendingPlay = true;
      pendingStartContextTime = jsCurrentTime(context);
      return;
    }
    playInternal();
  }

  @Override
  public void pause() {
    if (!playing) {
      return;
    }
    pauseOffset = cursorPositionDouble();
    stopNode();
    playing = false;
  }

  @Override
  public void stop() {
    stopNode();
    pauseOffset = 0.0;
    playing = false;
    ended = false;
    pendingPlay = false;
    pendingStartContextTime = -1.0;
  }

  @Override
  public boolean isPlaying() {
    return playing;
  }

  @Override
  public boolean isEnd() {
    return ended;
  }

  @Override
  public float getVolume() {
    return volume;
  }

  @Override
  public void setVolume(float volume) {
    this.volume = volume;
    jsSetGain(gainNode, volume);
  }

  @Override
  public void setPitch(float pitch) {
    this.pitch = pitch;
    if (sourceNode != null) {
      jsSetPlaybackRate(sourceNode, pitch);
    }
  }

  @Override
  public void setPan(float pan) {
    jsSetPan(pannerNode, pan);
  }

  @Override
  public float getCursorPosition() {
    return (float) cursorPositionDouble();
  }

  @Override
  public void seekTo(float seconds) {
    pauseOffset = seconds;
    if (playing) {
      stopNode();
      playing = false;
      playInternal();
    }
  }

  @Override
  public float getLength() {
    return (float) totalLength;
  }

  @Override
  public boolean isLooping() {
    return looping;
  }

  @Override
  public void setLooping(boolean looping) {
    this.looping = looping;
  }

  @Override
  public void setPosition(float x, float y, float z) {
    // No-op.
  }

  /**
   * Returns the stereo panner node, which is the final output stage of this sound.
   * Effect routing connects this to an effect's input (or reconnects it to the master gain
   * when the effect chain is cleared).
   *
   * @return The {@code StereoPannerNode} output node.
   */
  @Override
  public JSObject getOutputNode() {
    return pannerNode;
  }

  @Override
  public void dispose() {
    stop();
    jsDisconnect(gainNode);
    jsDisconnect(pannerNode);
    audioBuffer = null;
  }

  private void playInternal() {
    sourceNode = jsCreateBufferSource(context);
    jsSetBuffer(sourceNode, audioBuffer);
    jsSetLoop(sourceNode, looping);
    jsSetPlaybackRate(sourceNode, pitch);
    jsConnect(sourceNode, gainNode);
    jsSetOnEnded(sourceNode, this::onEnded);
    jsResumeIfSuspended(context);

    // If play() was called before decoding finished, compensate for how long decoding
    // took by skipping that many seconds into the audio. Every track that was started
    // in the same game frame records the same pendingStartContextTime, so they all skip
    // ahead by their individual decode lag and land at the same audio position.
    double startOffset = pauseOffset;
    if (pendingStartContextTime >= 0.0) {
      double lag = jsCurrentTime(context) - pendingStartContextTime;
      startOffset = pauseOffset + Math.max(0.0, lag);
      pendingStartContextTime = -1.0;
    }

    jsStartAt(sourceNode, startOffset);
    startContextTime = jsCurrentTime(context) - startOffset;
    playing = true;
  }

  private void onEnded() {
    if (manuallyStopped) {
      manuallyStopped = false;
      return;
    }
    if (!looping) {
      playing = false;
      pauseOffset = 0.0;
      ended = true;
    }
  }

  private void stopNode() {
    if (sourceNode != null) {
      manuallyStopped = true;
      jsStop(sourceNode);
      jsDisconnect(sourceNode);
      sourceNode = null;
    }
  }

  private double cursorPositionDouble() {
    if (playing) {
      double pos = jsCurrentTime(context) - startContextTime;
      if (!looping && totalLength > 0.0 && pos > totalLength) {
        return totalLength;
      }
      return pos;
    }
    return pauseOffset;
  }

  @JSBody(params = { "ctx" }, script = "return ctx.createGain();")
  private static native JSObject jsCreateGain(JSObject ctx);

  @JSBody(params = { "ctx" }, script = "return ctx.createStereoPanner();")
  private static native JSObject jsCreateStereoPanner(JSObject ctx);

  @JSBody(params = { "node", "v" }, script = "node.gain.value = v;")
  private static native void jsSetGain(JSObject node, float v);

  @JSBody(params = { "node", "v" }, script = "node.pan.value = v;")
  private static native void jsSetPan(JSObject node, float v);

  @JSBody(params = { "a", "b" }, script = "a.connect(b);")
  private static native void jsConnect(JSObject a, JSObject b);

  @JSBody(params = { "node" }, script = "try { node.disconnect(); } catch(e) {}")
  private static native void jsDisconnect(JSObject node);

  @JSBody(params = { "ctx", "buf", "onOk", "onErr" },
      script = "ctx.decodeAudioData(buf, onOk, onErr);")
  private static native void jsDecodeAudioData(JSObject ctx, ArrayBuffer buf,
      DecodeSuccessHandler onOk, DecodeErrorHandler onErr);

  @JSBody(params = { "ctx" }, script = "return ctx.createBufferSource();")
  private static native JSObject jsCreateBufferSource(JSObject ctx);

  @JSBody(params = { "src", "buf" }, script = "src.buffer = buf;")
  private static native void jsSetBuffer(JSObject src, JSObject buf);

  @JSBody(params = { "src", "loop" }, script = "src.loop = loop;")
  private static native void jsSetLoop(JSObject src, boolean loop);

  @JSBody(params = { "src", "rate" }, script = "src.playbackRate.value = rate;")
  private static native void jsSetPlaybackRate(JSObject src, float rate);

  @JSBody(params = { "src", "cb" }, script = "src.onended = cb;")
  private static native void jsSetOnEnded(JSObject src, EndedHandler cb);

  @JSBody(params = { "src", "offset" }, script = "src.start(0, offset);")
  private static native void jsStartAt(JSObject src, double offset);

  @JSBody(params = { "src" }, script = "try { src.stop(); } catch(e) {}")
  private static native void jsStop(JSObject src);

  @JSBody(params = { "ctx" }, script = "return ctx.currentTime;")
  private static native double jsCurrentTime(JSObject ctx);

  @JSBody(params = { "buf" }, script = "return buf.duration;")
  private static native double jsGetBufferDuration(JSObject buf);

  @JSBody(params = { "ctx" }, script = "if (ctx.state === 'suspended') ctx.resume();")
  private static native void jsResumeIfSuspended(JSObject ctx);

  /**
   * Callback fired once when async decoding of an {@code AudioBuffer} finishes.
   *
   * <p>Implemented by {@link FlixelTeaVMSoundHandler} to populate its per-path cache
   * so subsequent {@code createSound} calls for the same path skip decoding entirely.
   */
  interface AudioBufferCallback {

    /**
     * Called with the fully decoded buffer and its duration in seconds.
     *
     * @param buffer The decoded {@code AudioBuffer}.
     * @param length Duration of the audio in seconds.
     */
    void onDecoded(JSObject buffer, double length);
  }
}
