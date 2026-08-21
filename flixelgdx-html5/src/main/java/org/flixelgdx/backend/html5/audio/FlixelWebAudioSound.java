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
import org.flixelgdx.audio.FlixelLowPassEffect;
import org.flixelgdx.audio.FlixelReverbEffect;
import org.flixelgdx.audio.FlixelSound;
import org.flixelgdx.audio.FlixelSoundBuffer;
import org.flixelgdx.audio.FlixelSoundEffect;
import org.jetbrains.annotations.NotNull;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.Int8Array;
import org.teavm.jso.webaudio.AudioBuffer;
import org.teavm.jso.webaudio.AudioBufferSourceNode;
import org.teavm.jso.webaudio.AudioContext;
import org.teavm.jso.webaudio.GainNode;
import org.teavm.jso.webaudio.StereoPannerNode;

/**
 * A single playable sound backed by the Web Audio API.
 *
 * <p>Web Audio models playback in a way that shapes this whole class. Decoding is asynchronous: the
 * browser hands back the decoded samples through a callback some time after the sound is created,
 * so a {@code play()} that arrives before decoding finishes is remembered and started once the
 * samples are ready. A playing "voice" is a one-shot {@code AudioBufferSourceNode} that cannot be
 * paused or restarted, only stopped and replaced, so pausing here means stopping the current source
 * and recording where it was, and resuming means creating a fresh source that starts from that
 * saved position.
 *
 * <p>Volume and pan are steady controls, so they live on persistent gain and stereo-pan nodes that
 * every source connects through: {@code source -> pan -> gain -> master}. Pitch maps to the
 * source's {@code playbackRate}.
 *
 * <p>The reverb, echo, and low-pass effects are not implemented on the web backend and return the
 * framework's shared no-op effects, so effect calls are silently ignored rather than failing.
 */
public class FlixelWebAudioSound extends FlixelSound {

  private double startContextTime;
  private double cursorOffsetSeconds;

  @NotNull
  private final AudioContext context;

  @NotNull
  private final GainNode gainNode;

  @NotNull
  private final StereoPannerNode panNode;

  private final FlixelWebAudioGroup group;

  private AudioBuffer decodedBuffer;
  private AudioBufferSourceNode source;

  private float volume = 1f;
  private float pitch = 1f;
  private float pan;

  private boolean looping;
  private boolean playing;
  private boolean pendingPlay;
  private boolean suspendedByGroup;

  /**
   * Creates a sound and begins decoding its bytes in the background.
   *
   * @param context The shared audio context.
   * @param master The master gain node all sounds route into.
   * @param buffer The encoded audio bytes and their source path.
   * @param group The group this sound belongs to, or {@code null} for none.
   */
  public FlixelWebAudioSound(@NotNull AudioContext context, @NotNull GainNode master,
      @NotNull FlixelSoundBuffer buffer, FlixelWebAudioGroup group) {
    this.context = context;
    this.group = group;
    this.gainNode = context.createGain();
    this.panNode = context.createStereoPanner();

    FlixelWebAudioFactory.connect(panNode, gainNode);
    FlixelWebAudioFactory.connect(gainNode, master);

    if (group != null) {
      group.register(this);
    }

    decode(context, toArrayBuffer(buffer.data()), decoded -> {
      decodedBuffer = decoded;
      gainNode.getGain().setValue(volume);
      panNode.getPan().setValue(pan);
      if (pendingPlay) {
        pendingPlay = false;
        startSource(cursorOffsetSeconds);
      }
    });
  }

  @Override
  protected void backendPlay() {
    if (decodedBuffer == null) {
      pendingPlay = true;
      return;
    }
    startSource(cursorOffsetSeconds);
  }

  @Override
  protected void backendPause() {
    if (playing) {
      cursorOffsetSeconds = currentCursor();
      stopSource();
      playing = false;
    }
  }

  @Override
  protected void backendStop() {
    stopSource();
    cursorOffsetSeconds = 0.0;
    playing = false;
    pendingPlay = false;
  }

  @Override
  protected boolean backendIsPlaying() {
    return playing && !backendIsEnd();
  }

  @Override
  protected boolean backendIsEnd() {
    if (decodedBuffer == null || looping || !playing) {
      return false;
    }
    return currentCursor() >= decodedBuffer.getDuration();
  }

  @Override
  protected float backendGetVolume() {
    return volume;
  }

  @Override
  protected void backendSetVolume(float volume) {
    this.volume = volume;
    gainNode.getGain().setValue(volume);
  }

  @Override
  protected void backendSetPitch(float pitch) {
    // Re-anchor the cursor bookkeeping so the position stays continuous across a rate change.
    if (playing) {
      cursorOffsetSeconds = currentCursor();
      startContextTime = context.getCurrentTime();
    }
    this.pitch = pitch;
    if (source != null) {
      source.getPlaybackRate().setValue(pitch);
    }
  }

  @Override
  protected void backendSetPan(float pan) {
    this.pan = pan;
    panNode.getPan().setValue(pan);
  }

  @Override
  protected float backendGetCursor() {
    return (float) currentCursor();
  }

  @Override
  protected void backendSeek(float seconds) {
    cursorOffsetSeconds = seconds;
    if (playing) {
      startSource(seconds);
    }
  }

  @Override
  protected float backendGetLength() {
    return decodedBuffer != null ? (float) decodedBuffer.getDuration() : 0f;
  }

  @Override
  protected boolean backendIsLooping() {
    return looping;
  }

  @Override
  protected void backendSetLooping(boolean looping) {
    this.looping = looping;
    if (source != null) {
      source.setLoop(looping);
    }
  }

  @Override
  protected void backendSetPosition(float x, float y, float z) {
    // The web backend uses a stereo panner rather than a 3D spatializer, so positional audio is
    // not supported here.
  }

  @Override
  protected void backendDispose() {
    stopSource();
    if (group != null) {
      group.unregister(this);
    }
  }

  @Override
  @NotNull
  protected FlixelReverbEffect backendCreateReverb(float wet) {
    return FlixelReverbEffect.NOOP;
  }

  @Override
  @NotNull
  protected FlixelEchoEffect backendCreateEcho(float delaySeconds, float decay) {
    return FlixelEchoEffect.NOOP;
  }

  @Override
  @NotNull
  protected FlixelLowPassEffect backendCreateLowPass(double cutoffHz, int order) {
    return FlixelLowPassEffect.NOOP;
  }

  @Override
  protected void backendRouteTailToOutput(@NotNull FlixelSoundEffect tail) {}

  @Override
  protected void backendRestoreDirectRouting() {}

  /** Suspends this sound because its group was paused, remembering that the group did it. */
  void suspendForGroup() {
    if (playing) {
      backendPause();
      suspendedByGroup = true;
    }
  }

  /** Resumes this sound if it was suspended by its group being paused. */
  void resumeForGroup() {
    if (suspendedByGroup) {
      suspendedByGroup = false;
      backendPlay();
    }
  }

  /**
   * Starts a fresh source voice from the given position and updates the cursor anchor.
   *
   * @param offsetSeconds Where in the clip to begin playback.
   */
  private void startSource(double offsetSeconds) {
    stopSource();
    AudioBufferSourceNode node = context.createBufferSource();
    node.setBuffer(decodedBuffer);
    node.setLoop(looping);
    node.getPlaybackRate().setValue(pitch);
    FlixelWebAudioFactory.connect(node, panNode);
    node.start(0.0, offsetSeconds);

    source = node;
    startContextTime = context.getCurrentTime();
    cursorOffsetSeconds = offsetSeconds;
    playing = true;
  }

  /** Stops the current source voice, if any, tolerating an already-stopped node. */
  private void stopSource() {
    if (source != null) {
      safeStop(source);
      source = null;
    }
  }

  /**
   * Computes the current playback position in seconds from the cursor anchor and elapsed context
   * time, wrapping within the clip length while looping.
   *
   * @return The current cursor position in seconds.
   */
  private double currentCursor() {
    if (!playing) {
      return cursorOffsetSeconds;
    }
    double cursor = cursorOffsetSeconds + (context.getCurrentTime() - startContextTime) * pitch;
    if (looping && decodedBuffer != null) {
      double length = decodedBuffer.getDuration();
      if (length > 0.0) {
        cursor %= length;
      }
    }
    return cursor;
  }

  /**
   * Copies the encoded bytes into a JavaScript {@code ArrayBuffer} suitable for
   * {@code decodeAudioData}.
   *
   * @param data The encoded audio bytes.
   * @return An array buffer holding a copy of the bytes.
   */
  private static ArrayBuffer toArrayBuffer(byte[] data) {
    return Int8Array.copyFromJavaArray(data).getBuffer();
  }

  @JSBody(params = { "context", "buffer", "callback" },
      script = "context.decodeAudioData(buffer, function(decoded) { callback(decoded); }, function() {});")
  private static native void decode(AudioContext context, ArrayBuffer buffer, AudioBufferCallback callback);

  @JSBody(params = "source", script = "try { source.stop(); } catch (e) {}")
  private static native void safeStop(AudioBufferSourceNode source);

  /** Receives the decoded audio buffer once the browser finishes decoding. */
  @JSFunctor
  private interface AudioBufferCallback extends JSObject {
    void accept(AudioBuffer buffer);
  }
}
