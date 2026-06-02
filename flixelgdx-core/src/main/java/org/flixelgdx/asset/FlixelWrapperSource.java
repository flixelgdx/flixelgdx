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
 * An asset {@link FlixelSource} that also has a pooled Flixel wrapper type {@code W} (e.g.
 * {@link org.flixelgdx.graphics.FlixelGraphic} for textures).
 *
 * <p>Use {@link #obtainWrapper(FlixelAssetManager)} for the canonical wrapper without refcount changes
 * ({@link FlixelAssetManager#ensureWrapper}); use {@link FlixelAssetManager#obtainWrapper(String, Class)} when you need an implicit {@link FlixelAsset#retain()}.
 *
 * @param <T> Runtime type loaded from the asset manager (e.g. {@link com.badlogic.gdx.graphics.Texture}).
 * @param <W> Pooled wrapper type (e.g. {@link org.flixelgdx.graphics.FlixelGraphic}).
 */
public interface FlixelWrapperSource<T, W> extends FlixelSource<T> {

  /**
   * @return The wrapper class used with {@link FlixelAssetManager#ensureWrapper(String, Class)}.
   */
  @NotNull
  Class<W> wrapperType();

  /**
   * Returns or creates the pooled wrapper for this source’s key (does not change refcount).
   *
   * @param assets Asset manager that owns the wrapper cache.
   * @return Cached or new wrapper instance.
   */
  @NotNull
  default W obtainWrapper(@NotNull FlixelAssetManager assets) {
    return assets.ensureWrapper(getAssetKey(), wrapperType());
  }
}
