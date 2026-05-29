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
    // No-op.
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
    return 0;
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
    // No-op.
  }

  @Override
  public void dispose() {
    music.dispose();
  }
}
