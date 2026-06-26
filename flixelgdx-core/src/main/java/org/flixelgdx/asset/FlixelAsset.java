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
 * Unified handle for one asset, with reference counting and lifecycle policy.
 *
 * <p>All assets retrieved from {@link FlixelAssetManager} implement this interface.
 * {@link org.flixelgdx.graphics.FlixelGraphic FlixelGraphic} implements
 * {@code FlixelAsset<FlixelGraphic>} directly so the graphic object is the handle.
 * Other asset types use {@link FlixelDefaultAsset}.
 *
 * <p>Call {@link #retain()} when you take ownership of an asset handle and {@link #release()}
 * when you are done. The asset manager uses the reference count to decide when it is safe to
 * unload the underlying data during state switches.
 *
 * <p>Typical usage in a sprite class:
 *
 * <pre>{@code
 * // In a loading state
 * Flixel.assets.load("player.png");
 *
 * // In a game state
 * FlixelAsset<FlixelGraphic> asset = Flixel.assets.get("player.png");
 * asset.retain();
 * FlixelGraphic graphic = asset.get();
 *
 * // In destroy()
 * asset.release();
 * }</pre>
 *
 * @param <T> The wrapper type that game code interacts with (e.g.
 *   {@link org.flixelgdx.graphics.FlixelGraphic FlixelGraphic}).
 */
public interface FlixelAsset<T> {

  /**
   * Returns the normalized asset path this handle was created for.
   *
   * @return The asset path; never {@code null}.
   */
  @NotNull
  String getPath();

  /**
   * Returns the content for this asset, loading it synchronously if it is not ready yet.
   *
   * <p>Prefer loading assets ahead of time in a loading state via
   * {@link FlixelAssetManager#load(String)} and {@link FlixelAssetManager#update()} to avoid
   * mid-frame stalls caused by a synchronous load.
   *
   * @return The asset content; never {@code null}.
   */
  @NotNull
  T get();

  /**
   * Returns {@code true} if the content for this asset is already available without
   * triggering a synchronous load.
   *
   * @return {@code true} if {@link #get()} can return immediately without blocking.
   */
  boolean isLoaded();

  /**
   * Returns whether this asset survives {@link FlixelAssetManager#clearNonPersist()} when its
   * reference count is zero.
   *
   * @return {@code true} if persistent.
   */
  boolean isPersist();

  /**
   * Sets the persist flag for this asset. When {@code true}, the asset is kept in the manager
   * cache across state switches even when its reference count is zero.
   *
   * @param persist {@code true} to keep this asset across state switches when unreferenced.
   * @return {@code this} for chaining.
   */
  @NotNull
  FlixelAsset<T> setPersist(boolean persist);

  /**
   * Returns the current reference count. Zero means no owner holds this asset.
   *
   * @return The reference count.
   */
  int getRefCount();

  /**
   * Increments the reference count. Call this when you take ownership of the asset.
   *
   * @return {@code this} for chaining.
   */
  @NotNull
  FlixelAsset<T> retain();

  /**
   * Decrements the reference count. Call this when you are done with the asset. When the count
   * reaches zero, {@link FlixelAssetManager#onAssetReleased(FlixelAsset)} is called so the
   * manager can apply the active {@link FlixelAssetMode}.
   *
   * @return {@code this} for chaining.
   */
  @NotNull
  FlixelAsset<T> release();
}
