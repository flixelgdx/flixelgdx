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

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * General-purpose {@link FlixelAsset} handle for content that does not need its own wrapper
 * class, such as text files.
 *
 * <p>The handle looks its content up in the owning manager's raw cache on each
 * {@link #get()}, block-loading it when it was never queued. Types with richer behavior
 * ({@link org.flixelgdx.graphics.FlixelGraphic FlixelGraphic} for textures,
 * {@link org.flixelgdx.audio.FlixelSoundSource FlixelSoundSource} for audio) implement
 * {@link FlixelAsset} themselves instead.
 *
 * @param <T> The content type game code receives from {@link #get()} (e.g. {@link String}).
 */
public final class FlixelDefaultAsset<T> implements FlixelAsset<T> {

  @NotNull
  private final FlixelAssetManager assets;

  @NotNull
  private final String path;

  private int refCount;

  private boolean persist;

  /**
   * Creates a handle for {@code path}.
   *
   * @param assets The owning asset manager.
   * @param path Normalized asset path.
   */
  public FlixelDefaultAsset(@NotNull FlixelAssetManager assets, @NotNull String path) {
    this.assets = Objects.requireNonNull(assets, "assets cannot be null.");
    this.path = Objects.requireNonNull(path, "path cannot be null.");
    this.persist = assets.getGlobalPersist();
  }

  @NotNull
  @Override
  public String getPath() {
    return path;
  }

  @NotNull
  @Override
  @SuppressWarnings("unchecked")
  public T get() {
    Object raw = assets.getRaw(path);
    if (raw == null) {
      raw = assets.loadRawSync(path);
    }
    return (T) raw;
  }

  @Override
  public boolean isLoaded() {
    return assets.getRaw(path) != null;
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
}
