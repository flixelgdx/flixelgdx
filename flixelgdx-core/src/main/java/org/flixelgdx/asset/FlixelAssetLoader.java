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

import org.flixelgdx.file.FlixelFile;
import org.jetbrains.annotations.NotNull;

/**
 * Loads one kind of asset, in two stages, and wraps it in a {@link FlixelAsset} handle.
 * Register loaders per file extension via
 * {@link FlixelAssetManager#registerLoader(String, FlixelAssetLoader)}.
 *
 * <p>The two stages exist so platforms with threads can do the slow part in the background:
 *
 * <ol>
 *   <li>{@link #loadRaw(FlixelAssetManager, String, FlixelFile)} reads and parses the file into
 *     a <b>raw</b> object (decoded pixels, decoded audio, text). On platforms that support
 *     multithreading this runs on a worker thread, so it must not touch the GPU or other
 *     main-thread-only systems.</li>
 *   <li>{@link #finishRaw(FlixelAssetManager, String, Object)} runs on the main thread and may
 *     finalize the raw object (for example, uploading decoded pixels into a GPU texture). The
 *     default keeps the raw object as-is.</li>
 * </ol>
 *
 * <p>Separately, {@link #createHandle(FlixelAssetManager, String)} builds the <b>wrapper</b>
 * handle game code holds (a {@link org.flixelgdx.graphics.FlixelGraphic FlixelGraphic}, a
 * {@link FlixelDefaultAsset}, and so on). The handle reads the finished raw content back from
 * the manager's cache when first used, which keeps raw loading and wrapping one system instead
 * of two.
 *
 * <p>Example, a loader for a custom text-based config extension:
 *
 * <pre>{@code
 * Flixel.assets.registerLoader(".cfg", new FlixelAssetLoader<String>() {
 *   public Object loadRaw(FlixelAssetManager assets, String path, FlixelFile file) {
 *     return file.readString();
 *   }
 *
 *   public FlixelAsset<String> createHandle(FlixelAssetManager assets, String path) {
 *     return new FlixelDefaultAsset<>(assets, path);
 *   }
 * });
 * }</pre>
 *
 * <p>The framework registers loaders for image ({@code .png}, {@code .jpg}, {@code .jpeg},
 * {@code .bmp}, {@code .tga}, {@code .ktx2}), audio ({@code .mp3}, {@code .ogg}, {@code .wav},
 * {@code .flac}), and text ({@code .txt}, {@code .xml}, {@code .json}) files by default.
 *
 * @param <T> The wrapper type that game code receives from {@link FlixelAsset#get()}.
 */
public interface FlixelAssetLoader<T> {

  /**
   * Stage one: reads and parses the file into a raw object.
   *
   * <p>On platforms with threads this may run on a worker thread; do not touch the GPU or any
   * main-thread-only system here. Decode into CPU-side data ({@link org.flixelgdx.graphics.FlixelImage
   * FlixelImage}, byte arrays, strings) and let {@link #finishRaw} do main-thread work.
   *
   * @param assets The owning asset manager.
   * @param path Normalized asset path (e.g. {@code "images/player.png"}).
   * @param file The resolved file to read.
   * @return The raw parsed content; never {@code null}.
   * @throws Exception If reading or parsing fails; the manager logs and skips the asset.
   */
  @NotNull
  Object loadRaw(@NotNull FlixelAssetManager assets, @NotNull String path, @NotNull FlixelFile file) throws Exception;

  /**
   * Stage two: finalizes the raw object on the main thread.
   *
   * <p>The default returns the raw object unchanged. Override when finalization needs the main
   * thread, such as uploading decoded pixels into a GPU texture.
   *
   * @param assets The owning asset manager.
   * @param path Normalized asset path.
   * @param raw The object produced by {@link #loadRaw}.
   * @return The finished raw content stored in the manager's cache; never {@code null}.
   */
  @NotNull
  default Object finishRaw(@NotNull FlixelAssetManager assets, @NotNull String path, @NotNull Object raw) {
    return raw;
  }

  /**
   * Creates the {@link FlixelAsset} wrapper handle for {@code path}.
   *
   * <p>Handles are created lazily and cached by the manager; the handle fetches the finished
   * raw content from the manager when first used.
   *
   * @param assets The owning asset manager.
   * @param path Normalized asset path.
   * @return A new handle; never {@code null}.
   */
  @NotNull
  FlixelAsset<T> createHandle(@NotNull FlixelAssetManager assets, @NotNull String path);
}
