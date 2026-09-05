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
 * shared {@link FlixelSound} base; this subclass only forwards audio operations to the
 * {@link FlixelMiniAudio} native bridge.
 *
 * <p>The effect-node factory methods return the no-op sentinels for now: miniaudio's node graph
 * is a follow-up slice, so reverb/echo/low-pass are accepted but do nothing rather than failing.
 */
public class FlixelMiniAudioSound extends FlixelSound {

  /** Native miniaudio sound handle, or {@code 0} once disposed. */
  private long handle;

  /** Cached pitch; miniaudio has no pitch getter. */
  private float pitch = 1f;

  /** Cached pan; miniaudio has no pan getter. */
  private float pan = 0f;

  /**
   * Wraps a native sound handle.
   *
   * @param handle The native handle from {@link FlixelMiniAudio#soundLoad}.
   */
  FlixelMiniAudioSound(long handle) {
    this.handle = handle;
  }

  @NotNull
  @Override
  public FlixelSound resume() {
    if (handle != 0L) {
      FlixelMiniAudio.soundStart(handle);
    }
    return this;
  }

  @NotNull
  @Override
  public FlixelSound pause() {
    if (handle != 0L) {
      FlixelMiniAudio.soundStop(handle);
    }
    return this;
  }

  @Override
  protected boolean isEnd() {
    return handle == 0L || FlixelMiniAudio.soundIsAtEnd(handle);
  }

  @Override
  public float getVolume() {
    return handle == 0L ? 0f : FlixelMiniAudio.soundGetVolume(handle);
  }

  @Override
  public FlixelSound setVolume(float volume) {
    if (handle != 0L) {
      FlixelMiniAudio.soundSetVolume(handle, volume);
    }
    return this;
  }

  @Override
  public float getPitch() {
    return pitch;
  }

  @Override
  public FlixelSound setPitch(float pitch) {
    this.pitch = pitch;
    if (handle != 0L) {
      FlixelMiniAudio.soundSetPitch(handle, pitch);
    }
    return this;
  }

  @Override
  public float getPan() {
    return pan;
  }

  @Override
  public FlixelSound setPan(float pan) {
    this.pan = pan;
    if (handle != 0L) {
      FlixelMiniAudio.soundSetPan(handle, pan);
    }
    return this;
  }

  @Override
  public float getTime() {
    return handle == 0L ? 0f : FlixelMiniAudio.soundGetCursor(handle) * 1000f;
  }

  @Override
  public FlixelSound setTime(float timeMs) {
    if (handle != 0L) {
      FlixelMiniAudio.soundSeek(handle, timeMs / 1000f);
    }
    return this;
  }

  @Override
  public float getLength() {
    return handle == 0L ? 0f : FlixelMiniAudio.soundGetLength(handle) * 1000f;
  }

  @Override
  public boolean isLooped() {
    return handle != 0L && FlixelMiniAudio.soundIsLooping(handle);
  }

  @Override
  public FlixelSound setLooped(boolean looped) {
    if (handle != 0L) {
      FlixelMiniAudio.soundSetLooping(handle, looped);
    }
    return this;
  }

  @Override
  public boolean isPlaying() {
    return handle != 0L && FlixelMiniAudio.soundIsPlaying(handle);
  }

  @Override
  protected void applyPosition(float x, float y, float z) {
    if (handle != 0L) {
      FlixelMiniAudio.soundSetPosition(handle, x, y, z);
    }
  }

  @Override
  protected void disposeAudio() {
    if (handle != 0L) {
      FlixelMiniAudio.soundUninit(handle);
      handle = 0L;
    }
  }

  @NotNull
  @Override
  protected FlixelReverbEffect createReverbEffect(float wet) {
    return FlixelReverbEffect.NOOP;
  }

  @NotNull
  @Override
  protected FlixelEchoEffect createEchoEffect(float delaySeconds, float decay) {
    return FlixelEchoEffect.NOOP;
  }

  @NotNull
  @Override
  protected FlixelLowPassEffect createLowPassEffect(double cutoffHz, int order) {
    return FlixelLowPassEffect.NOOP;
  }

  @Override
  protected void routeEffectToOutput(@NotNull FlixelSoundEffect tail) {}

  @Override
  protected void restoreDirectRouting() {}
}
