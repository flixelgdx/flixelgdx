/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.asset;

import org.jetbrains.annotations.NotNull;

/**
 * Typed handle for one asset path + class, with optional {@code persist} and refcount.
 *
 * <p>Obtain pooled handles with {@link FlixelAssetManager#obtainTypedAsset(String, Class)} (cached on
 * {@link me.stringdotjar.flixelgdx.Flixel#assets}). {@link me.stringdotjar.flixelgdx.graphics.FlixelGraphic} and
 * {@link me.stringdotjar.flixelgdx.audio.FlixelSound} also implement this contract where applicable.
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
