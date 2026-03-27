/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.asset;

import org.jetbrains.annotations.NotNull;

/**
 * An asset {@link FlixelSource} that also has a pooled Flixel wrapper type {@code W} (e.g.
 * {@link me.stringdotjar.flixelgdx.graphics.FlixelGraphic} for textures).
 *
 * <p>Use {@link #obtainWrapper(FlixelAssetManager)} to get the canonical wrapper for {@link #getAssetKey()};
 * the manager does not add texture- or type-specific methods—only {@link FlixelAssetManager#obtainWrapper(String, Class)}.
 *
 * @param <T> Runtime type loaded from the asset manager (e.g. {@link com.badlogic.gdx.graphics.Texture}).
 * @param <W> Pooled wrapper type (e.g. {@link me.stringdotjar.flixelgdx.graphics.FlixelGraphic}).
 */
public interface FlixelWrapperSource<T, W> extends FlixelSource<T> {

  /**
   * @return The wrapper class used with {@link FlixelAssetManager#obtainWrapper(String, Class)}.
   */
  @NotNull
  Class<W> wrapperType();

  /**
   * Returns or creates the pooled wrapper for this source’s key (does not {@code retain}).
   *
   * @param assets Asset manager that owns the wrapper cache.
   * @return Cached or new wrapper instance.
   */
  @NotNull
  default W obtainWrapper(@NotNull FlixelAssetManager assets) {
    return assets.obtainWrapper(getAssetKey(), wrapperType());
  }
}
