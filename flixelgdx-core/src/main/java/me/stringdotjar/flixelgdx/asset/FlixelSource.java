/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.asset;

import org.jetbrains.annotations.NotNull;

/**
 * Common interface for asset "source" objects.
 *
 * <p>A source is a lightweight reference to one asset, identified by an asset key and a type.
 * This allows {@link FlixelAssetManager} to provide consistent overloads for loading and requiring
 * assets, while letting developers create their own source classes for custom workflows.
 *
 * <p>Sources should not implicitly retain or acquire ownership of pooled wrappers. Keep ownership
 * explicit in the consumer (for example, a sprite that stores a {@code FlixelGraphic} should call
 * {@code retain()} when it starts using it and {@code release()} when done).
 */
public interface FlixelSource<T> {

  @NotNull
  String getAssetKey();

  @NotNull
  Class<T> getType();

  /**
   * Enqueues this source into the provided asset manager. Safe to call multiple times.
   *
   * @param assets The asset manager to enqueue this source into.
   */
  default void queueLoad(@NotNull FlixelAssetManager assets) {
    assets.load(getAssetKey(), getType());
  }

  /**
   * Whether this source is loaded in the provided asset manager.
   *
   * @param assets The asset manager to check if this source is loaded in.
   * @return {@code true} if this source is loaded in the provided asset manager, {@code false} otherwise.
   */
  default boolean isLoaded(@NotNull FlixelAssetManager assets) {
    return assets.isLoaded(getAssetKey(), getType());
  }

  /**
   * Requires {@code this} source to already be loaded in the provided asset manager.
   *
   * @param assets The asset manager to require this source from.
   * @return The required source.
   */
  @NotNull
  default T require(@NotNull FlixelAssetManager assets) {
    if (!isLoaded(assets)) {
      throw new IllegalStateException(
        "Asset not loaded: \"" + getAssetKey() + "\" (" + getType().getSimpleName() + ")."
      );
    }
    return assets.get(getAssetKey(), getType());
  }
}

