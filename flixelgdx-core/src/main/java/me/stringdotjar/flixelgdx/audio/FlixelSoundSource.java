/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.audio;

import com.badlogic.gdx.files.FileHandle;
import games.rednblack.miniaudio.MAGroup;
import games.rednblack.miniaudio.MASound;
import me.stringdotjar.flixelgdx.Flixel;
import me.stringdotjar.flixelgdx.asset.FlixelSource;
import me.stringdotjar.flixelgdx.util.FlixelPathsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Cached sound "source" (asset) that can spawn fresh {@link FlixelSound} instances on demand.
 *
 * <p>Do not cache {@link FlixelSound} playback objects directly: a playback object has mutable state
 * (volume/pan/time/playing) and cannot be safely shared across callers or overlapping plays.
 */
public final class FlixelSoundSource implements FlixelSource<FlixelSoundSource> {

  @NotNull
  private final String assetKey;

  private final boolean external;

  public FlixelSoundSource(@NotNull String assetKey) {
    this(assetKey, false);
  }

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

  public boolean isExternal() {
    return external;
  }

  /** Creates a new playable {@link FlixelSound} instance using the provided group (or SFX group if null). */
  @NotNull
  public FlixelSound create(@Nullable MAGroup group) {
    String resolvedPath = external ? assetKey : FlixelPathsUtil.resolveAudioPath(assetKey);
    MAGroup targetGroup = (group != null) ? group : Flixel.sound.getSfxGroup();

    MASound ma = Flixel.getAudioEngine().createSound(resolvedPath, (short) 0, targetGroup, external);
    return new FlixelSound(ma);
  }

  /** Convenience overload using the default SFX group. */
  @NotNull
  public FlixelSound create() {
    return create(null);
  }

  /** Convenience constructor from a libGDX file handle (uses {@code handle.path()} as key). */
  @NotNull
  public static FlixelSoundSource fromFile(@NotNull FileHandle handle) {
    return new FlixelSoundSource(handle.path(), false);
  }
}

