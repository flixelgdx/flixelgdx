/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.backend.jvm.audio;

import games.rednblack.miniaudio.MASound;
import me.stringdotjar.flixelgdx.audio.FlixelSoundBackend;

/**
 * JVM implementation of {@link FlixelSoundBackend} that wraps a single
 * MiniAudio {@link MASound}.
 */
final class FlixelMiniAudioSound implements FlixelSoundBackend {

  private final MASound sound;

  FlixelMiniAudioSound(MASound sound) {
    this.sound = sound;
  }

  /** Returns the underlying {@link MASound} for advanced engine operations. */
  MASound getMASound() {
    return sound;
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
    sound.setVolume(volume);
  }

  @Override
  public void setPitch(float pitch) {
    sound.setPitch(pitch);
  }

  @Override
  public void setPan(float pan) {
    sound.setPan(pan);
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
