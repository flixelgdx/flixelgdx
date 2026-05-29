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
package me.stringdotjar.flixelgdx.asset;

import org.jetbrains.annotations.NotNull;

/**
 * Typed handle for one asset path + class, with optional {@code persist} and refcount.
 *
 * <p>Handles are cached on {@link me.stringdotjar.flixelgdx.Flixel#assets}. {@link FlixelAssetManager#obtainTypedAsset(String, Class)}
 * and {@link FlixelAssetManager#obtainWrapper(String, Class)} implicitly {@link #retain()}; use {@link FlixelAssetManager#ensureTypedAsset}
 * / {@link FlixelAssetManager#ensureWrapper} when you must not change the refcount. {@link me.stringdotjar.flixelgdx.graphics.FlixelGraphic}
 * and {@link me.stringdotjar.flixelgdx.audio.FlixelSound} also implement this contract where applicable.
 *
 * <p>Prefer {@link #queueLoad()} in a loading state and {@code Flixel.assets.update()} each frame.
 *
 * @param <T> LibGDX-loaded asset type (e.g. {@link com.badlogic.gdx.graphics.Texture}).
 */
public interface FlixelAsset<T> {

  /**
   * Gets and returns the asset key associated with this asset.
   *
   * @return The asset key.
   */
  @NotNull
  String getAssetKey();

  /**
   * Gets and returns the type of the asset.
   *
   * @return The asset type.
   */
  @NotNull
  Class<T> getType();

  /**
   * Checks if {@code this} asset will be kept in memory after the game state is switched.
   *
   * @return {@code true} if the asset will be kept in memory after the game state is switched, {@code false} otherwise.
   */
  boolean isPersist();

  /**
   * Sets whether {@code this} asset will be kept in memory after the game state is switched.
   *
   * @param persist {@code true} if the asset will be kept in memory after the game state is switched, {@code false} otherwise.
   * @return {@code this} asset for chaining.
   */
  @NotNull
  FlixelAsset<T> setPersist(boolean persist);

  /**
   * Gets and returns the reference count of {@code this} asset.
   *
   * @return The reference count.
   */
  int getRefCount();

  /**
   * Increments the reference count of {@code this} asset.
   *
   * @return {@code this} asset for chaining.
   */
  @NotNull
  FlixelAsset<T> retain();

  /**
   * Decrements the reference count of {@code this} asset.
   *
   * @return {@code this} asset for chaining.
   */
  @NotNull
  FlixelAsset<T> release();

  /**
   * Enqueues this asset into the active {@link FlixelAssetManager}. Safe to call multiple times.
   *
   * <p>Use this method to preload assets in your game's loading state.
   */
  void queueLoad();

  /**
   * Requires the asset to already be loaded.
   *
   * @return The loaded asset.
   * @throws IllegalStateException if the asset is not loaded.
   */
  @NotNull
  T require();

  /**
   * Explicit synchronous load for one-off cases. Avoid using implicitly during gameplay.
   */
  @NotNull
  T loadNow();
}
