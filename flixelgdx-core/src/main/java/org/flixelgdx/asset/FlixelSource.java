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

