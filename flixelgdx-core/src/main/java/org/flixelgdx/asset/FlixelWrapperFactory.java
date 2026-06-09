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

import java.util.function.Consumer;

/**
 * Creates and pools wrapper instances for a single wrapper type. Register with
 * {@link FlixelAssetManager#registerWrapperFactory(FlixelWrapperFactory)}.
 *
 * <p>Implementations for their object map should make its key type as {@link String}, and its
 * value type as the wrapper object {@link W}.
 *
 * @param <W> Concrete wrapper type (e.g. {@link org.flixelgdx.graphics.FlixelGraphic}).
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

  /** Inserts a caller-constructed wrapper (e.g. owned resource) into the pool under {@link FlixelPooledWrapper#getAssetKey()}. */
  void registerInstance(@NotNull FlixelAssetManager assets, @NotNull W wrapper);

  /** Disposes all non-persistent wrapper objects. */
  void clearNonPersist(@NotNull FlixelAssetManager assets);

  /** Accepts a {@link Consumer} and iterates through the cached objects in {@code this} factory. */
  void forEachWrappedAsset(Consumer<W> consumer);

  /**
   * Disposes and evicts a single wrapper by key. For owned wrappers, the dedicated resource is
   * disposed immediately. For path-keyed wrappers, the asset is unloaded from the underlying
   * {@link com.badlogic.gdx.assets.AssetManager} if it is currently loaded.
   *
   * <p>This is the targeted single-entry counterpart to {@link #clearNonPersist(FlixelAssetManager)}.
   * It is called by {@link FlixelAssetManager#onAssetReleased(FlixelAsset)} in
   * {@link FlixelAssetMode#AGGRESSIVE} mode to evict a wrapper the moment its reference count
   * reaches zero, without scanning the whole cache.
   *
   * @param assets The manager that owns this factory.
   * @param key The asset key of the wrapper to evict.
   */
  void evict(@NotNull FlixelAssetManager assets, @NotNull String key);

  /**
   * Disposes and evicts all wrapper objects. Path-keyed assets should be unloaded from the
   * underlying {@link com.badlogic.gdx.assets.AssetManager} when applicable.
   *
   * @param assets The manager that owns this factory, used to reach the libGDX {@code AssetManager}.
   */
  void clearAll(@NotNull FlixelAssetManager assets);
}
