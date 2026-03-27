/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.asset;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Default pooled implementation of {@link FlixelAsset}; constructed only via
 * {@link FlixelAssetManager#obtainTypedAsset(String, Class)} or framework subclasses.
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

  private boolean persist;
  private int refCount;

  /**
   * Same-package and subclass access for {@link FlixelDefaultAssetManager} and typed wrappers such as
   * {@link me.stringdotjar.flixelgdx.graphics.FlixelGraphic}.
   */
  protected FlixelTypedAsset(
    @NotNull FlixelAssetManager assetManager,
    @NotNull String assetKey,
    @NotNull Class<T> type
  ) {
    if (assetManager == null) {
      throw new IllegalArgumentException("Asset manager cannot be null.");
    }
    if (assetKey == null || assetKey.isEmpty()) {
      throw new IllegalArgumentException("Asset key cannot be null/empty.");
    }
    if (type == null) {
      throw new IllegalArgumentException("Type cannot be null.");
    }
    this.assetManager = assetManager;
    this.assetKey = assetKey;
    this.type = type;
    this.persist = false;
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
    refCount--;
    if (refCount < 0) {
      refCount = 0;
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
          + "Preload it in a loading state (Flixel.assets.load + Flixel.assets.update), or call loadNow() explicitly."
      );
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
    return assetManager.obtainTypedAsset(otherAssetKey, otherType);
  }

  @Nullable
  public FlixelAsset<?> peek(@NotNull String otherAssetKey, @NotNull Class<?> otherType) {
    return assetManager.peekTypedAsset(otherAssetKey, otherType);
  }
}
