/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.backend.teavm.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import me.stringdotjar.flixelgdx.audio.FlixelSoundBackend;

/**
 * TeaVM/web implementation of {@link FlixelSoundBackend} backed by libGDX
 * {@link Music}. {@code Music} is used instead of {@code Sound} because it
 * exposes position, duration, looping, and pause/resume controls that the
 * FlixelGDX audio API requires.
 */
final class FlixelGdxSound implements FlixelSoundBackend {

  private final Music music;
  private float volume = 1f;

  FlixelGdxSound(String path, boolean external) {
    if (external) {
      music = Gdx.audio.newMusic(Gdx.files.absolute(path));
    } else {
      music = Gdx.audio.newMusic(Gdx.files.internal(path));
    }
  }

  @Override
  public void play() {
    music.play();
  }

  @Override
  public void pause() {
    music.pause();
  }

  @Override
  public void stop() {
    music.stop();
  }

  @Override
  public boolean isPlaying() {
    return music.isPlaying();
  }

  @Override
  public boolean isEnd() {
    return !music.isPlaying() && music.getPosition() <= 0f;
  }

  @Override
  public float getVolume() {
    return volume;
  }

  @Override
  public void setVolume(float volume) {
    this.volume = volume;
    music.setVolume(volume);
  }

  @Override
  public void setPitch(float pitch) {
    throw new UnsupportedOperationException("Pitch is not supported on TeaVM");
  }

  @Override
  public void setPan(float pan) {
    music.setPan(pan, volume);
  }

  @Override
  public float getCursorPosition() {
    return music.getPosition();
  }

  @Override
  public void seekTo(float seconds) {
    music.setPosition(seconds);
  }

  @Override
  public float getLength() {
    throw new UnsupportedOperationException("Duration is not supported on TeaVM");
  }

  @Override
  public boolean isLooping() {
    return music.isLooping();
  }

  @Override
  public void setLooping(boolean looping) {
    music.setLooping(looping);
  }

  @Override
  public void setPosition(float x, float y, float z) {
    throw new UnsupportedOperationException("Spatial audio is not supported on TeaVM");
  }

  @Override
  public void dispose() {
    music.dispose();
  }
}
