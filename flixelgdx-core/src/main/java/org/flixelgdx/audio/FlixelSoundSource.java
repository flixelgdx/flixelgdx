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

import com.badlogic.gdx.files.FileHandle;

import org.flixelgdx.Flixel;
import org.flixelgdx.asset.FlixelAssetPaths;
import org.flixelgdx.util.FlixelPathsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Cached sound "source" (asset) that can spawn fresh {@link FlixelSound} instances on demand.
 *
 * <p>Do not cache {@link FlixelSound} playback objects directly: a playback
 * object has mutable state (volume/pan/time/playing) and cannot be safely
 * shared across callers or overlapping plays.
 */
public final class FlixelSoundSource {

  @NotNull
  private final String assetKey;

  private final boolean external;

  /**
   * Creates a sound source with the given asset key.
   *
   * @param assetKey The path or key identifying the audio asset.
   */
  public FlixelSoundSource(@NotNull String assetKey) {
    this(assetKey, false);
  }

  /**
   * Creates a sound source with the given asset key and external flag.
   *
   * @param assetKey The path or key identifying the audio asset.
   * @param external {@code true} if the path is an absolute external path.
   */
  public FlixelSoundSource(@NotNull String assetKey, boolean external) {
    if (assetKey == null || assetKey.isEmpty()) {
      throw new IllegalArgumentException("assetKey cannot be null/empty");
    }
    this.assetKey = external ? assetKey : FlixelAssetPaths.normalizeAssetPath(assetKey);
    this.external = external;
  }

  /**
   * Returns the asset key (path) for this sound source.
   *
   * @return The asset key; never {@code null}.
   */
  @NotNull
  public String getAssetKey() {
    return assetKey;
  }

  /**
   * Returns whether this source uses an external (absolute) path.
   *
   * @return {@code true} if external.
   */
  public boolean isExternal() {
    return external;
  }

  /**
   * Creates a new playable {@link FlixelSound} instance using the provided
   * group (or the default SFX group if {@code null}).
   *
   * @param group Group handle from the backend factory, or {@code null}.
   * @return A new sound instance.
   */
  @NotNull
  public FlixelSound create(@Nullable Object group) {
    String resolvedPath = external ? assetKey : FlixelPathsUtil.resolveAudioPath(assetKey);
    Object targetGroup = (group != null) ? group : Flixel.sound.getSfxGroup();
    FlixelSoundBackend.Factory factory = Flixel.soundFactory;
    FlixelSoundBackend backend = factory.createSound(resolvedPath, (short) 0, targetGroup, external);
    return new FlixelSound(backend);
  }

  /**
   * Convenience overload using the default SFX group.
   *
   * @return A new sound instance.
   */
  @NotNull
  public FlixelSound create() {
    return create(null);
  }

  /**
   * Convenience constructor from a libGDX file handle (uses {@code handle.path()} as key).
   *
   * @param handle The file handle to the audio file.
   * @return A new sound source.
   */
  @NotNull
  public static FlixelSoundSource fromFile(@NotNull FileHandle handle) {
    return new FlixelSoundSource(handle.path(), false);
  }
}
