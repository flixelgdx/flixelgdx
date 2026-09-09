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

import org.flixelgdx.Flixel;
import org.flixelgdx.asset.FlixelAsset;
import org.flixelgdx.asset.FlixelAssetLoader;
import org.flixelgdx.asset.FlixelAssetManager;
import org.flixelgdx.file.FlixelFile;
import org.jetbrains.annotations.NotNull;

/**
 * Asset loader for audio files ({@code .mp3}, {@code .ogg}, {@code .wav}, {@code .flac}).
 *
 * <p>Stage one reads the encoded file into a {@link FlixelSoundBuffer} (on a worker thread
 * where supported) and asks the active {@link FlixelSoundFactory} to pre-decode it, so the
 * first play has no decode lag. The wrapper handle is a {@link FlixelSoundSource}, which spawns
 * fresh {@link FlixelSound} instances on demand.
 *
 * <p>{@link FlixelSoundManager} registers this loader for the audio extensions when it is
 * created; games only interact with it indirectly through {@code Flixel.assets.load(...)}.
 */
public final class FlixelSoundSourceLoader implements FlixelAssetLoader<FlixelSoundSource> {

  @NotNull
  @Override
  public Object loadRaw(@NotNull FlixelAssetManager assets, @NotNull String path, @NotNull FlixelFile file) {
    FlixelSoundBuffer buffer = FlixelSoundBuffer.read(path, file);
    if (Flixel.sound != null) {
      Flixel.sound.getFactory().prewarm(buffer);
    }
    return buffer;
  }

  @NotNull
  @Override
  public FlixelAsset<FlixelSoundSource> createHandle(@NotNull FlixelAssetManager assets, @NotNull String path) {
    return new FlixelSoundSource(assets, path);
  }
}
