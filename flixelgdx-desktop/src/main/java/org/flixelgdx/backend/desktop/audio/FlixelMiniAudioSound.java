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
package org.flixelgdx.backend.desktop.audio;

import org.flixelgdx.audio.FlixelEchoEffect;
import org.flixelgdx.audio.FlixelLowPassEffect;
import org.flixelgdx.audio.FlixelReverbEffect;
import org.flixelgdx.audio.FlixelSound;
import org.flixelgdx.audio.FlixelSoundEffect;
import org.jetbrains.annotations.NotNull;

/**
 * A {@link FlixelSound} backed by one miniaudio voice.
 *
 * <p>Every gameplay-facing behavior (fades, completion signals, effect bookkeeping) lives in the
 * shared {@link FlixelSound} base; this subclass only forwards the small set of backend
 * primitives to the {@link FlixelMiniAudio} native bridge.
 *
 * <p>The effect-node factory methods return the no-op sentinels for now: miniaudio's node graph
 * is a follow-up slice, so reverb/echo/low-pass are accepted but do nothing rather than failing.
 */
public class FlixelMiniAudioSound extends FlixelSound {

  /** Native miniaudio sound handle, or {@code 0} once disposed. */
  private long handle;

  /**
   * Wraps a native sound handle.
   *
   * @param handle The native handle from {@link FlixelMiniAudio#soundLoad}.
   */
  FlixelMiniAudioSound(long handle) {
    this.handle = handle;
  }

  @Override
  protected void backendPlay() {
    if (handle != 0L) {
      FlixelMiniAudio.soundStart(handle);
    }
  }

  @Override
  protected void backendPause() {
    if (handle != 0L) {
      FlixelMiniAudio.soundStop(handle);
    }
  }

  @Override
  protected void backendStop() {
    if (handle != 0L) {
      FlixelMiniAudio.soundStop(handle);
      FlixelMiniAudio.soundSeek(handle, 0f);
    }
  }

  @Override
  protected boolean backendIsPlaying() {
    return handle != 0L && FlixelMiniAudio.soundIsPlaying(handle);
  }

  @Override
  protected boolean backendIsEnd() {
    return handle == 0L || FlixelMiniAudio.soundIsAtEnd(handle);
  }

  @Override
  protected float backendGetVolume() {
    return handle == 0L ? 0f : FlixelMiniAudio.soundGetVolume(handle);
  }

  @Override
  protected void backendSetVolume(float volume) {
    if (handle != 0L) {
      FlixelMiniAudio.soundSetVolume(handle, volume);
    }
  }

  @Override
  protected void backendSetPitch(float pitch) {
    if (handle != 0L) {
      FlixelMiniAudio.soundSetPitch(handle, pitch);
    }
  }

  @Override
  protected void backendSetPan(float pan) {
    if (handle != 0L) {
      FlixelMiniAudio.soundSetPan(handle, pan);
    }
  }

  @Override
  protected float backendGetCursor() {
    return handle == 0L ? 0f : FlixelMiniAudio.soundGetCursor(handle);
  }

  @Override
  protected void backendSeek(float seconds) {
    if (handle != 0L) {
      FlixelMiniAudio.soundSeek(handle, seconds);
    }
  }

  @Override
  protected float backendGetLength() {
    return handle == 0L ? 0f : FlixelMiniAudio.soundGetLength(handle);
  }

  @Override
  protected boolean backendIsLooping() {
    return handle != 0L && FlixelMiniAudio.soundIsLooping(handle);
  }

  @Override
  protected void backendSetLooping(boolean looping) {
    if (handle != 0L) {
      FlixelMiniAudio.soundSetLooping(handle, looping);
    }
  }

  @Override
  protected void backendSetPosition(float x, float y, float z) {
    if (handle != 0L) {
      FlixelMiniAudio.soundSetPosition(handle, x, y, z);
    }
  }

  @Override
  protected void backendDispose() {
    if (handle != 0L) {
      FlixelMiniAudio.soundUninit(handle);
      handle = 0L;
    }
  }

  @NotNull
  @Override
  protected FlixelReverbEffect backendCreateReverb(float wet) {
    return FlixelReverbEffect.NOOP;
  }

  @NotNull
  @Override
  protected FlixelEchoEffect backendCreateEcho(float delaySeconds, float decay) {
    return FlixelEchoEffect.NOOP;
  }

  @NotNull
  @Override
  protected FlixelLowPassEffect backendCreateLowPass(double cutoffHz, int order) {
    return FlixelLowPassEffect.NOOP;
  }

  @Override
  protected void backendRouteTailToOutput(@NotNull FlixelSoundEffect tail) {}

  @Override
  protected void backendRestoreDirectRouting() {}
}
