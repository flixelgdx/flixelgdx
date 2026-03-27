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
 * Creates and pools wrapper instances for a single wrapper type. Register with
 * {@link FlixelAssetManager#registerWrapperFactory(FlixelWrapperFactory)}.
 *
 * @param <W> Concrete wrapper type (e.g. {@link me.stringdotjar.flixelgdx.graphics.FlixelGraphic}).
 */
public interface FlixelWrapperFactory<W> {

  @NotNull
  Class<W> wrapperType();

  /** Returns a wrapper object if it is cached, otherwise creates a new one and caches it. */
  @NotNull
  W obtainKeyed(@NotNull FlixelAssetManager assets, @NotNull String key);

  /** Returns a wrapper object if it is cached, otherwise returns {@code null}. */
  @Nullable
  W peek(@NotNull FlixelAssetManager assets, @NotNull String key);

  /**
   * Inserts a caller-constructed wrapper (e.g. owned resource) into the pool under {@link FlixelPooledWrapper#getAssetKey()}.
   */
  void registerInstance(@NotNull FlixelAssetManager assets, @NotNull W wrapper);

  /** Disposes all non-persistent wrapper objects. */
  void clearNonPersist(@NotNull FlixelAssetManager assets);

  /** Disposes all wrapper objects. */
  void clearAll();
}
