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

import org.flixelgdx.asset.FlixelAsset;
import org.flixelgdx.asset.FlixelAssetManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Reference-counted wrapper around a libGDX {@link Texture}, implementing
 * {@link FlixelAsset}{@code <FlixelGraphic>}.
 *
 * <p>Graphics are shared: multiple sprites loading the same path get the same
 * {@code FlixelGraphic} instance from {@link FlixelAssetManager#get(String)}. Each user must
 * call {@link #retain()} on the handle and {@link #release()} when done so the manager can
 * track which textures are still in use and unload idle ones at state-switch time.
 *
 * <p>Use {@link #getTexture()} to access the underlying libGDX {@link Texture}. If the
 * texture is not yet loaded it is fetched synchronously; queue the asset with
 * {@link FlixelAssetManager#load(String)} in a loading state to avoid mid-frame stalls.
 *
 * <p><b>Owned vs path-keyed graphics</b>
 * <ul>
 *   <li><b>Path-keyed</b> - Created from a file path (e.g. {@code "images/player.png"}).
 *     The texture is managed by the libGDX {@link com.badlogic.gdx.assets.AssetManager}
 *     and unloaded when the reference count drops to zero and {@link FlixelAssetManager#clearNonPersist()} runs.</li>
 *   <li><b>Owned</b> - Created with a dedicated {@link Texture} (e.g. from
 *     {@link org.flixelgdx.FlixelSprite#makeGraphic FlixelSprite.makeGraphic}). The texture is
 *     disposed directly when the graphic is evicted. {@link #isOwned()} is {@code true}.</li>
 * </ul>
 *
 * <p><b>Persist</b> controls whether an unreferenced path-keyed graphic survives
 * {@link FlixelAssetManager#clearNonPersist()}. Owned graphics always use
 * {@code persist = false} and are evicted at refcount zero regardless.
 */
public final class FlixelGraphic implements FlixelAsset<FlixelGraphic> {

  @NotNull
  private final FlixelAssetManager assets;

  @NotNull
  private final String path;

  @Nullable
  private final Texture ownedTexture;

  private int refCount;

  private final boolean owned;

  private boolean persist;

  /**
   * Creates a path-keyed graphic. The texture is loaded lazily from the libGDX
   * {@link com.badlogic.gdx.assets.AssetManager} on the first {@link #getTexture()} call.
   *
   * @param assets The owning asset manager.
   * @param path Normalized asset path.
   */
  public FlixelGraphic(@NotNull FlixelAssetManager assets, @NotNull String path) {
    this(assets, path, null);
  }

  /**
   * Creates an owned graphic wrapping a dedicated texture (e.g. from a
   * {@link com.badlogic.gdx.graphics.Pixmap}). Pass {@code null} for {@code ownedTexture}
   * to create a path-keyed graphic instead.
   *
   * @param assets The owning asset manager.
   * @param path Asset path or synthetic key.
   * @param ownedTexture The dedicated texture, or {@code null} for path-keyed loading.
   */
  public FlixelGraphic(
      @NotNull FlixelAssetManager assets,
      @NotNull String path,
      @Nullable Texture ownedTexture) {
    this.assets = Objects.requireNonNull(assets, "assets cannot be null.");
    this.path = Objects.requireNonNull(path, "path cannot be null.");
    this.ownedTexture = ownedTexture;
    this.owned = (ownedTexture != null);
    this.persist = !owned && assets.getGlobalPersist();
  }

  @NotNull
  @Override
  public String getPath() {
    return path;
  }

  /** Returns {@code this}, since the graphic is its own handle. */
  @NotNull
  @Override
  public FlixelGraphic get() {
    return this;
  }

  @Override
  public boolean isLoaded() {
    return owned || assets.getManager().isLoaded(getResolvedPath(), Texture.class);
  }

  /** Returns whether this graphic's texture has been loaded into memory. */
  public boolean getLoaded() {
    return isLoaded();
  }

  @Override
  public boolean isPersist() {
    return persist;
  }

  /** Returns whether this graphic is marked to persist across state transitions. */
  public boolean getPersist() {
    return persist;
  }

  @NotNull
  @Override
  public FlixelGraphic setPersist(boolean persist) {
    if (!owned) {
      this.persist = persist;
    }
    return this;
  }

  @Override
  public int getRefCount() {
    return refCount;
  }

  @NotNull
  @Override
  public FlixelGraphic retain() {
    refCount++;
    return this;
  }

  @NotNull
  @Override
  public FlixelGraphic release() {
    if (refCount <= 0) {
      refCount = 0;
      return this;
    }
    refCount--;
    if (refCount == 0) {
      assets.onAssetReleased(this);
    }
    return this;
  }

  /**
   * Returns the underlying libGDX {@link Texture}. If the texture is not yet loaded, it is
   * fetched synchronously. Prefer loading assets in a loading state to avoid stalls.
   *
   * @return The texture; never {@code null}.
   */
  @NotNull
  public Texture getTexture() {
    if (owned) {
      return Objects.requireNonNull(ownedTexture, "Owned texture is null.");
    }
    String resolvedPath = getResolvedPath();
    if (!assets.getManager().isLoaded(resolvedPath, Texture.class)) {
      assets.getManager().load(resolvedPath, Texture.class);
      assets.finishLoadingAsset(resolvedPath);
    }
    return assets.getManager().get(resolvedPath, Texture.class);
  }

  /**
   * Returns the path actually used to load the underlying texture from the libGDX
   * {@code AssetManager}, resolved on demand through {@link FlixelAssetManager#resolveTexturePath}.
   *
   * <p>Equal to {@link #getPath()} unless a {@code .ktx2} sibling was found for this graphic, in
   * which case that sibling's path is returned instead.
   *
   * @return The resolved texture path.
   */
  @NotNull
  public String getResolvedPath() {
    return owned ? path : assets.resolveTexturePath(path);
  }

  /**
   * Returns the dedicated texture for owned graphics, or {@code null} for path-keyed ones.
   *
   * @return The owned texture, or {@code null}.
   */
  @Nullable
  public Texture getOwnedTexture() {
    return ownedTexture;
  }

  /**
   * Returns {@code true} if this graphic wraps a dedicated texture (e.g. from
   * {@link org.flixelgdx.FlixelSprite#makeGraphic FlixelSprite.makeGraphic}) that is disposed
   * directly when the graphic is evicted.
   *
   * @return {@code true} if owned.
   */
  public boolean isOwned() {
    return owned;
  }

  /** Returns whether this graphic owns its texture and disposes it on eviction. */
  public boolean getOwned() {
    return owned;
  }
}
