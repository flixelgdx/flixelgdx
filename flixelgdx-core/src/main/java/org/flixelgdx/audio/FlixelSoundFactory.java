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
package org.flixelgdx.audio;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The audio backend's entry point: creates sounds and groups and controls the engine.
 *
 * <p>One implementation is installed on {@link FlixelSoundManager} per platform (miniaudio via
 * JNI on desktop and Android, the Web Audio API on web) before
 * {@link org.flixelgdx.Flixel#start Flixel.start} runs. Game code rarely touches the factory
 * directly; it plays audio through {@code Flixel.sound.play(...)} and
 * {@code Flixel.sound.playMusic(...)}, or creates silent-until-played instances with
 * {@code Flixel.sound.create(...)}.
 *
 * <p>Sounds are created from in-memory {@link FlixelSoundBuffer}s read through the file seam,
 * never from raw path strings, so audio behaves identically packaged and unpackaged.
 */
public interface FlixelSoundFactory {

  /**
   * Creates a new sound from encoded audio bytes. The sound starts idle; call
   * {@link FlixelSound#play()} to hear it.
   *
   * @param buffer The encoded audio file contents.
   * @param group The group the sound belongs to, or {@code null} for the backend default.
   * @return A new backend sound; never {@code null}.
   */
  @NotNull
  FlixelSound createSound(@NotNull FlixelSoundBuffer buffer, @Nullable FlixelSoundGroup group);

  /**
   * Creates a new sound group for batch pause/resume control.
   *
   * @return A new group; never {@code null}.
   */
  @NotNull
  FlixelSoundGroup createGroup();

  /**
   * Sets the global master volume for the audio engine.
   *
   * @param volume Master volume in [0, 1].
   */
  void setMasterVolume(float volume);

  /** Shuts the underlying audio engine down and releases all native resources. */
  void destroyEngine();

  /**
   * Pre-decodes a buffer so the next {@link #createSound} call for the same path starts with no
   * decode lag. On platforms where decoding is synchronous this is a no-op; the web backend
   * decodes in the background.
   *
   * @param buffer The audio to pre-decode.
   */
  default void prewarm(@NotNull FlixelSoundBuffer buffer) {}

  /**
   * Returns {@code true} while at least one {@link #prewarm} decode is still in progress, so
   * loading screens can wait for audio to be truly ready.
   *
   * @return {@code true} if one or more background decodes have not yet completed.
   */
  default boolean isPrewarmPending() {
    return false;
  }
}
