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

import org.flixelgdx.Flixel;
import org.flixelgdx.audio.FlixelNoopSoundFactory;
import org.flixelgdx.audio.FlixelSound;
import org.flixelgdx.audio.FlixelSoundBuffer;
import org.flixelgdx.audio.FlixelSoundFactory;
import org.flixelgdx.audio.FlixelSoundGroup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The desktop audio backend: creates miniaudio-backed sounds and groups.
 *
 * <p>Install it via {@link #create()}, which loads the bundled native library and initializes the
 * engine. If the native cannot be loaded, {@code create()} returns the silent
 * {@link FlixelNoopSoundFactory} instead, so a missing or incompatible native never crashes the
 * game - it just plays no sound.
 *
 * <p>Sounds are created from in-memory {@link FlixelSoundBuffer}s (the encoded file bytes read
 * through the file seam), which miniaudio decodes internally. This keeps audio working the same
 * from a folder, a packaged JAR, or any other file root.
 */
public class FlixelMiniAudioFactory implements FlixelSoundFactory {

  /** Native miniaudio engine handle. */
  private final long engine;

  private FlixelMiniAudioFactory(long engine) {
    this.engine = engine;
  }

  /**
   * Loads the native library, initializes the engine, and returns the factory.
   *
   * @return A miniaudio factory, or the silent {@link FlixelNoopSoundFactory} when the native
   *     library or engine could not be brought up.
   */
  @NotNull
  public static FlixelSoundFactory create() {
    if (!FlixelMiniAudio.ensureLoaded()) {
      return FlixelNoopSoundFactory.INSTANCE;
    }
    long engine = FlixelMiniAudio.engineInit();
    if (engine == 0L) {
      Flixel.warn("Audio", "miniaudio engine failed to initialize; audio is disabled.");
      return FlixelNoopSoundFactory.INSTANCE;
    }
    return new FlixelMiniAudioFactory(engine);
  }

  @NotNull
  @Override
  public FlixelSound createSound(@NotNull FlixelSoundBuffer buffer, @Nullable FlixelSoundGroup group) {
    long groupHandle = (group instanceof FlixelMiniAudioGroup g) ? g.getHandle() : 0L;
    byte[] data = buffer.data();
    long handle = FlixelMiniAudio.soundLoad(engine, data, data.length, groupHandle);
    if (handle == 0L) {
      Flixel.warn("Audio", "Could not decode audio '" + buffer.path() + "'.");
    }
    return new FlixelMiniAudioSound(handle);
  }

  @NotNull
  @Override
  public FlixelSoundGroup createGroup() {
    return new FlixelMiniAudioGroup(FlixelMiniAudio.groupInit(engine));
  }

  @Override
  public void setMasterVolume(float volume) {
    FlixelMiniAudio.engineSetVolume(engine, volume);
  }

  @Override
  public void destroyEngine() {
    FlixelMiniAudio.engineUninit(engine);
  }
}
