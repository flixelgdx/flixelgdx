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
package org.flixelgdx.asset;

import com.badlogic.gdx.assets.AssetManager;
import org.flixelgdx.audio.FlixelSoundSource;
import org.flixelgdx.graphics.FlixelGraphic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Generic {@link FlixelAsset} handle for types whose libGDX raw type is the same as the content
 * type (e.g. {@link String} for text files, {@link FlixelSoundSource FlixelSoundSource} for audio).
 *
 * <p>Content is resolved lazily: the first {@link #get()} call either fetches the value from the
 * libGDX {@link AssetManager} once loading completes, or triggers a synchronous load if the asset
 * was not queued first. Prefer {@link FlixelAssetManager#load(String)} in a loading state to avoid
 * mid-frame stalls.
 *
 * <p>For image assets, use {@link FlixelGraphic FlixelGraphic}, which implements
 * {@link FlixelAsset}{@code <FlixelGraphic>} directly.
 *
 * @param <T> The content type; must match what the registered libGDX loader produces.
 */
public final class FlixelDefaultAsset<T> implements FlixelAsset<T> {

  @NotNull
  private final FlixelAssetManager assets;

  @NotNull
  private final String path;

  @NotNull
  private final Class<?> rawType;

  @Nullable
  private T content;

  private int refCount;

  private boolean persist;

  /**
   * Creates a new handle. Content is not fetched until {@link #get()} is first called.
   *
   * @param assets The owning asset manager.
   * @param path Normalized asset path.
   * @param rawType The libGDX raw type registered with the underlying {@link AssetManager} (e.g. {@code String.class}).
   */
  public FlixelDefaultAsset(
      @NotNull FlixelAssetManager assets,
      @NotNull String path,
      @NotNull Class<?> rawType) {
    this.assets = Objects.requireNonNull(assets, "assets cannot be null.");
    this.path = Objects.requireNonNull(path, "path cannot be null.");
    this.rawType = Objects.requireNonNull(rawType, "rawType cannot be null.");
    this.persist = assets.getGlobalPersist();
  }

  @NotNull
  @Override
  public String getPath() {
    return path;
  }

  /**
   * Returns the content for this asset, loading it synchronously if not yet ready.
   *
   * <p>When the asset was queued via {@link FlixelAssetManager#load(String)} and
   * {@link FlixelAssetManager#update()} has finished, this returns immediately. Otherwise
   * it blocks the current thread to load the asset.
   */
  @NotNull
  @Override
  @SuppressWarnings("unchecked")
  public T get() {
    if (content == null) {
      if (!assets.getManager().isLoaded(path, rawType)) {
        assets.getManager().load(path, (Class<Object>) rawType);
        assets.getManager().finishLoadingAsset(path);
      }
      content = assets.getManager().get(path, (Class<T>) rawType);
    }
    return content;
  }

  @Override
  public boolean isLoaded() {
    return content != null || assets.getManager().isLoaded(path, rawType);
  }

  @Override
  public boolean isPersist() {
    return persist;
  }

  @NotNull
  @Override
  public FlixelDefaultAsset<T> setPersist(boolean persist) {
    this.persist = persist;
    return this;
  }

  @Override
  public int getRefCount() {
    return refCount;
  }

  @NotNull
  @Override
  public FlixelDefaultAsset<T> retain() {
    refCount++;
    return this;
  }

  @NotNull
  @Override
  public FlixelDefaultAsset<T> release() {
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
   * Returns the libGDX raw type used for loading and unloading from the underlying
   * {@link com.badlogic.gdx.assets.AssetManager}.
   */
  @NotNull
  Class<?> getRawType() {
    return rawType;
  }
}
