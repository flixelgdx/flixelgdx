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
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Default pooled implementation of {@link FlixelAsset}; constructed only via
 * {@link FlixelAssetManager#ensureTypedAsset(String, Class)} / {@link FlixelAssetManager#obtainTypedAsset(String, Class)}
 * or framework subclasses. New handles default to {@link FlixelAssetManager#getGlobalPersist()} so {@code clearNonPersist}
 * either keeps or drops unreferenced handles accordingly. Call {@link #setPersist(boolean)} to override per handle.
 *
 * @param <T> Asset type.
 */
public class FlixelTypedAsset<T> implements FlixelAsset<T> {

  @NotNull
  private final String assetKey;

  @NotNull
  private final Class<T> type;

  @NotNull
  private final FlixelAssetManager assetManager;

  private int refCount;
  private boolean persist;

  /**
   * Same-package and subclass access for {@link FlixelDefaultAssetManager} and typed wrappers such as
   * {@link org.flixelgdx.graphics.FlixelGraphic}.
   */
  protected FlixelTypedAsset(
      @NotNull FlixelAssetManager assetManager,
      @NotNull String assetKey,
      @NotNull Class<T> type) {
    Objects.requireNonNull(assetManager, "Asset manager cannot be null.");
    Objects.requireNonNull(assetKey, "Asset key cannot be null/empty.");
    Objects.requireNonNull(type, "Type cannot be null.");
    this.assetManager = assetManager;
    this.assetKey = assetKey;
    this.type = type;
    this.persist = assetManager.getGlobalPersist();
    this.refCount = 0;
  }

  @NotNull
  @Override
  public String getAssetKey() {
    return assetKey;
  }

  @NotNull
  @Override
  public Class<T> getType() {
    return type;
  }

  @Override
  public boolean isPersist() {
    return persist;
  }

  @NotNull
  @Override
  public FlixelTypedAsset<T> setPersist(boolean persist) {
    this.persist = persist;
    return this;
  }

  @Override
  public int getRefCount() {
    return refCount;
  }

  @NotNull
  @Override
  public FlixelTypedAsset<T> retain() {
    refCount++;
    return this;
  }

  @NotNull
  @Override
  public FlixelTypedAsset<T> release() {
    if (refCount <= 0) {
      refCount = 0;
      return this;
    }
    refCount--;
    if (refCount == 0) {
      assetManager.onAssetReleased(this);
    }
    return this;
  }

  @Override
  public void queueLoad() {
    if (!assetManager.isLoaded(assetKey, type)) {
      assetManager.load(assetKey, type);
    }
  }

  @NotNull
  @Override
  public T require() {
    if (!assetManager.isLoaded(assetKey, type)) {
      throw new IllegalStateException(
          "Asset not loaded: \"" + assetKey + "\" (" + type.getSimpleName() + "). "
              + "Preload it in a loading state (Flixel.assets.load + Flixel.assets.update), or call loadNow() explicitly.");
    }
    return assetManager.get(assetKey, type);
  }

  @NotNull
  @Override
  public T loadNow() {
    if (!assetManager.isLoaded(assetKey, type)) {
      assetManager.load(assetKey, type);
      assetManager.finishLoadingAsset(assetKey);
    }
    return assetManager.get(assetKey, type);
  }

  /**
   * Obtains another typed handle from the manager (same pattern as the former instance helpers on {@code FlixelAsset}).
   */
  @NotNull
  public FlixelAsset<T> get(@NotNull String otherAssetKey, @NotNull Class<T> otherType) {
    return assetManager.ensureTypedAsset(otherAssetKey, otherType);
  }

  @Nullable
  public FlixelAsset<?> peek(@NotNull String otherAssetKey, @NotNull Class<?> otherType) {
    return assetManager.peekTypedAsset(otherAssetKey, otherType);
  }
}
