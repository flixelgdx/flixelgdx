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
package org.flixelgdx.graphics;

import com.badlogic.gdx.graphics.Texture;

import org.flixelgdx.Flixel;
import org.flixelgdx.asset.FlixelAssetManager;
import org.flixelgdx.asset.FlixelAssetPaths;
import org.flixelgdx.asset.FlixelWrapperSource;
import org.jetbrains.annotations.NotNull;

/**
 * Cached graphic "source" (asset) that can provide a pooled {@link FlixelGraphic} wrapper.
 *
 * <p>{@link #get()} uses {@link FlixelAssetManager#ensureWrapper} (no refcount change).
 * {@link #acquire()} uses {@link FlixelAssetManager#obtainWrapper} (implicit {@link org.flixelgdx.asset.FlixelAsset#retain()}).
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
    this.assetKey = FlixelAssetPaths.normalizeAssetPath(assetKey);
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

  /** Returns the pooled wrapper for this asset key (does not change refcount). */
  @NotNull
  public FlixelGraphic get() {
    return Flixel.ensureAssets().ensureWrapper(assetKey, FlixelGraphic.class);
  }

  /** Returns the pooled wrapper with implicit {@link org.flixelgdx.asset.FlixelAsset#retain()}. */
  @NotNull
  public FlixelGraphic acquire() {
    return Flixel.ensureAssets().obtainWrapper(assetKey, FlixelGraphic.class);
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
