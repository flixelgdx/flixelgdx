/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.graphics;

import com.badlogic.gdx.graphics.Texture;

import me.stringdotjar.flixelgdx.Flixel;
import me.stringdotjar.flixelgdx.asset.FlixelAssetManager;
import me.stringdotjar.flixelgdx.asset.FlixelWrapperSource;

import org.jetbrains.annotations.NotNull;

/**
 * Cached graphic "source" (asset) that can provide a pooled {@link FlixelGraphic} wrapper.
 *
 * <p>Ownership is explicit: {@link #get()} does not retain, while {@link #acquire()} retains.
 *
 * <p>Uses generic {@link FlixelAssetManager#obtainWrapper(String, Class)} via {@link FlixelWrapperSource};
 * the loaded texture is required with {@link #require(FlixelAssetManager)} (same as {@link #requireTexture(FlixelAssetManager)}).
 */
public final class FlixelGraphicSource implements FlixelWrapperSource<Texture, FlixelGraphic> {

  @NotNull
  private final String assetKey;

  public FlixelGraphicSource(@NotNull String assetKey) {
    if (assetKey == null || assetKey.isEmpty()) {
      throw new IllegalArgumentException("Asset key cannot be null/empty.");
    }
    this.assetKey = assetKey;
  }

  @Override
  public String getAssetKey() {
    return assetKey;
  }

  @Override
  public Class<Texture> getType() {
    return Texture.class;
  }

  @Override
  public Class<FlixelGraphic> wrapperType() {
    return FlixelGraphic.class;
  }

  /** Returns the pooled wrapper for this asset key (does not retain). */
  @NotNull
  public FlixelGraphic get() {
    return obtainWrapper(Flixel.ensureAssets());
  }

  /** Returns the pooled wrapper and retains it (explicit ownership). */
  @NotNull
  public FlixelGraphic acquire() {
    return obtainWrapper(Flixel.ensureAssets()).retain();
  }

  /**
   * Requires the underlying texture to already be loaded, then returns it.
   *
   * <p>Equivalent to {@link #require(FlixelAssetManager)}.
   */
  @NotNull
  public Texture requireTexture(@NotNull FlixelAssetManager assets) {
    return require(assets);
  }
}
