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
 * Creates a {@link FlixelAsset} handle for one asset path. Register loaders per file extension
 * via {@link FlixelAssetManager#registerLoader(String, Class, FlixelAssetLoader)}.
 *
 * <p>This is a functional interface, so lambdas work for simple cases:
 *
 * <pre>{@code
 * Flixel.assets.registerLoader(".cfg", String.class,
 *     (assets, path) -> new FlixelDefaultAsset<>(assets, path, String.class));
 * }</pre>
 *
 * <p>The framework registers loaders for image ({@code .png}, {@code .jpg}, {@code .jpeg},
 * {@code .webp}), audio ({@code .mp3}, {@code .ogg}, {@code .wav}, {@code .flac}), and text
 * ({@code .txt}, {@code .xml}, {@code .json}) files by default. Use
 * {@link FlixelAssetManager#registerLoader} to override or add new extensions.
 *
 * @param <T> The wrapper type that game code receives from {@link FlixelAsset#get()}.
 */
@FunctionalInterface
public interface FlixelAssetLoader<T> {

  /**
   * Creates a new {@link FlixelAsset} handle for {@code path}.
   *
   * <p>Content is typically resolved lazily: the handle is created here, but the actual
   * data is not fetched until {@link FlixelAsset#get()} is first called. This allows the
   * libGDX {@link com.badlogic.gdx.assets.AssetManager} to finish loading in the background
   * before the handle is used.
   *
   * @param assets The owning asset manager.
   * @param path Normalized asset path (e.g. {@code "images/player.png"}).
   * @return A new handle; never {@code null}.
   */
  @NotNull
  FlixelAsset<T> create(@NotNull FlixelAssetManager assets, @NotNull String path);
}
