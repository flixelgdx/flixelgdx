/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.audio;

import com.badlogic.gdx.files.FileHandle;
import me.stringdotjar.flixelgdx.Flixel;
import me.stringdotjar.flixelgdx.asset.FlixelSource;
import me.stringdotjar.flixelgdx.util.FlixelPathsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Cached sound "source" (asset) that can spawn fresh {@link FlixelSound} instances on demand.
 *
 * <p>Do not cache {@link FlixelSound} playback objects directly: a playback
 * object has mutable state (volume/pan/time/playing) and cannot be safely
 * shared across callers or overlapping plays.
 */
public final class FlixelSoundSource implements FlixelSource<FlixelSoundSource> {

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
    this.assetKey = assetKey;
    this.external = external;
  }

  @Override
  public String getAssetKey() {
    return assetKey;
  }

  @Override
  public Class<FlixelSoundSource> getType() {
    return FlixelSoundSource.class;
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
    FlixelSoundBackend.Factory factory = Flixel.getSoundFactory();
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
   * Convenience constructor from a libGDX file handle (uses
   * {@code handle.path()} as key).
   *
   * @param handle The file handle to the audio file.
   * @return A new sound source.
   */
  @NotNull
  public static FlixelSoundSource fromFile(@NotNull FileHandle handle) {
    return new FlixelSoundSource(handle.path(), false);
  }
}
