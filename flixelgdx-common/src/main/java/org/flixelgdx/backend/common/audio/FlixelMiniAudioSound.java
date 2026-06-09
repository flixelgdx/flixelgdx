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
package org.flixelgdx.backend.common.audio;

import org.flixelgdx.audio.FlixelSoundBackend;
import org.jetbrains.annotations.Nullable;

import games.rednblack.miniaudio.MAGroup;
import games.rednblack.miniaudio.MASound;

/**
 * MiniAudio-backed {@link FlixelSoundBackend} that wraps a single {@link MASound}.
 */
final class FlixelMiniAudioSound implements FlixelSoundBackend {

  private final MASound sound;

  @Nullable
  private final MAGroup group;

  FlixelMiniAudioSound(MASound sound, @Nullable MAGroup group) {
    this.sound = sound;
    this.group = group;
  }

  /** Returns the underlying {@link MASound} for advanced engine operations. */
  MASound getMASound() {
    return sound;
  }

  /**
   * Returns the group this sound was created in, or {@code null} if it has no group.
   * Used by the factory to route effect chains through the group so that group-level
   * pause and resume still apply even when effects are active.
   *
   * @return The group, or {@code null}.
   */
  @Nullable
  MAGroup getGroup() {
    return group;
  }

  @Override
  public void play() {
    sound.play();
  }

  @Override
  public void pause() {
    sound.pause();
  }

  @Override
  public void stop() {
    sound.stop();
  }

  @Override
  public boolean isPlaying() {
    return sound.isPlaying();
  }

  @Override
  public boolean isEnd() {
    return sound.isEnd();
  }

  @Override
  public float getVolume() {
    return sound.getVolume();
  }

  @Override
  public void setVolume(float volume) {
    sound.setVolume(Math.min(Math.max(volume, 0f), 1f));
  }

  @Override
  public void setPitch(float pitch) {
    sound.setPitch(Math.max(0f, pitch));
  }

  @Override
  public void setPan(float pan) {
    sound.setPan(Math.min(Math.max(pan, -1f), 1f));
  }

  @Override
  public float getCursorPosition() {
    return sound.getCursorPosition();
  }

  @Override
  public void seekTo(float seconds) {
    sound.seekTo(seconds);
  }

  @Override
  public float getLength() {
    return sound.getLength();
  }

  @Override
  public boolean isLooping() {
    return sound.isLooping();
  }

  @Override
  public void setLooping(boolean looping) {
    sound.setLooping(looping);
  }

  @Override
  public void setPosition(float x, float y, float z) {
    sound.setPosition(x, y, z);
  }

  @Override
  public void dispose() {
    sound.dispose();
  }
}
