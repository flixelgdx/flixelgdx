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
import org.flixelgdx.file.FlixelFiles;
import org.flixelgdx.functional.FlixelDestroyable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Asset manager interface for FlixelGDX.
 *
 * <p>This is the public seam used by sprites and other runtime systems. It is a pure interface:
 * each platform installs its own implementation (the shared JVM one for desktop and Android, a
 * browser-based one for web), and a safe no-op ({@link FlixelNoopAssetManager}) is in place
 * before any backend starts. Access via {@link org.flixelgdx.Flixel#assets Flixel.assets}.
 *
 * <p><b>Basic workflow:</b>
 *
 * <pre>{@code
 * // Loading state: queue assets
 * Flixel.assets.load("player.png");
 * Flixel.assets.load("music/bg.mp3");
 *
 * // Each frame until done
 * Flixel.assets.update();
 *
 * // Game state: get and retain a handle
 * FlixelAsset<FlixelGraphic> graphic = Flixel.assets.get("player.png");
 * graphic.retain();
 *
 * // Use the content
 * sprite.loadGraphic(graphic.get());
 *
 * // Release when done (e.g. in destroy())
 * graphic.release();
 * }</pre>
 *
 * <p>{@link #load(String)} infers the asset type from the file extension using the
 * per-manager loader registry ({@link #registerLoader}). On platforms that support
 * multithreading, queued assets load asynchronously on worker threads and finish (for example,
 * upload to the GPU) during {@link #update()} on the main thread.
 *
 * <p>Path keys are normalized (collapsed slashes, unified separators) via
 * {@link FlixelAssetPaths#normalizeAssetPath(String)} so duplicate slashes or backslashes
 * do not cause mismatches on web builds or other backends where paths are compared literally.
 *
 * <p><b>Files, not strings.</b> Every read goes through the {@link FlixelFiles} seam: the
 * manager turns each path into a {@link FlixelFile} with its file resolver (by default,
 * {@code Flixel.files.internal(path)}). Games that must also run from inside a packaged JAR
 * should install a resolver that switches the root type, which is slightly more to write but
 * keeps every asset read consistent and safe:
 *
 * <pre>{@code
 * // In your launcher, before Flixel.start(...):
 * Flixel.assets.setFileResolver(path -> {
 *   FlixelFile onDisk = Flixel.files.internal(path);
 *   return onDisk.exists() ? onDisk : Flixel.files.classpath(path);
 * });
 * }</pre>
 */
public interface FlixelAssetManager extends FlixelDestroyable {

  /**
   * Queues an asset for loading using the file extension to select a loader from the registry.
   *
   * <p>Call {@link #update()} each frame (or {@link #finishLoading()} to block) until loading
   * completes, then retrieve the handle with {@link #get(String)}.
   *
   * @param path Asset path (e.g. {@code "images/player.png"}).
   * @throws IllegalArgumentException if the path has no extension or no loader is registered.
   */
  void load(@NotNull String path);

  /**
   * Like {@link #load(String)}, but marks the first handle created for this key as persistent.
   * Persistent handles survive {@link #clearNonPersist()} when their reference count is zero.
   *
   * @param path Asset path.
   * @param persist When {@code true}, the first handle created for this path is persistent.
   */
  void load(@NotNull String path, boolean persist);

  /**
   * Returns the {@link FlixelAsset} handle for {@code path}, creating it if it does not exist.
   *
   * <p>The handle's content is resolved lazily: {@link FlixelAsset#get()} fetches the data
   * once loading completes or triggers a synchronous load if needed. Call {@link #load(String)}
   * in a loading state first to avoid mid-frame stalls.
   *
   * <p>Multiple calls with the same path return the same cached handle. Call
   * {@link FlixelAsset#retain()} when you take ownership and {@link FlixelAsset#release()} when
   * done so the manager can track which assets are still in use.
   *
   * <p>The return type is inferred from the loader registered for the path's extension. The
   * cast is unchecked internally; passing the wrong type variable at the call site will produce
   * a {@link ClassCastException} at runtime if the inferred type does not match.
   *
   * @param path Asset path.
   * @param <T> Expected wrapper type (e.g. {@link org.flixelgdx.graphics.FlixelGraphic FlixelGraphic}).
   * @return The cached or newly created handle; never {@code null}.
   * @throws IllegalArgumentException if no loader is registered for the path's extension.
   */
  @NotNull
  <T> FlixelAsset<T> get(@NotNull String path);

  /**
   * Returns the cached {@link FlixelAsset} handle for {@code path} without creating one.
   *
   * <p>Use this for read-only checks, for example to see if a shared resource is already
   * registered before creating it. Does not change the reference count.
   *
   * @param path Asset path.
   * @return The cached handle, or {@code null} if none exists.
   */
  @Nullable
  FlixelAsset<?> peek(@NotNull String path);

  /**
   * Registers a loader for a file extension.
   *
   * <p>Example: adding a custom config extension that loads text:
   *
   * <pre>{@code
   * Flixel.assets.registerLoader(".cfg", myConfigLoader);
   * }</pre>
   *
   * @param extension File extension with or without a leading dot (e.g. {@code ".png"} or {@code "png"}).
   * @param loader Loads content and creates handles for paths with this extension.
   * @param <T> The wrapper type the loader produces.
   */
  <T> void registerLoader(@NotNull String extension, @NotNull FlixelAssetLoader<T> loader);

  /**
   * Removes a previously registered loader for the given extension.
   *
   * @param extension Same form as {@link #registerLoader}.
   */
  void unregisterLoader(@NotNull String extension);

  /**
   * Registers a caller-constructed asset handle directly with the manager cache. Use this for
   * assets created outside the normal loading pipeline (e.g. a texture built from a
   * {@link org.flixelgdx.graphics.FlixelImage FlixelImage}).
   *
   * <p>The handle is keyed by {@link FlixelAsset#getPath()}. If a handle is already registered
   * under that key, it is replaced.
   *
   * @param asset The handle to register. Use {@link #allocateSyntheticKey()} when no natural
   *   path exists.
   */
  void register(@NotNull FlixelAsset<?> asset);

  /**
   * Allocates a unique synthetic key for caller-created assets that have no natural path.
   * Use with {@link #register(FlixelAsset)}.
   *
   * @return A unique key string; never {@code null}.
   */
  @NotNull
  String allocateSyntheticKey();

  /**
   * Advances the loading pipeline: finishes assets whose background work completed (uploading
   * textures, caching decoded audio) and, on single-threaded platforms, loads the next queued
   * asset. Call once per frame in a loading state.
   *
   * @return {@code true} when all queued loading is finished.
   */
  boolean update();

  /**
   * Advances the loading pipeline for up to {@code millis} milliseconds.
   *
   * @param millis Maximum time to spend updating.
   * @return {@code true} when all queued loading is finished.
   */
  boolean update(int millis);

  /**
   * Returns overall loading progress in {@code [0, 1]}.
   *
   * @return Progress fraction.
   */
  float getProgress();

  /**
   * Returns the number of assets currently tracked in the manager cache.
   *
   * <p>A steadily climbing count across state switches often means assets are being loaded
   * without a matching {@link FlixelAsset#release()} or {@link #clearNonPersist()} call.
   *
   * @return Number of cached handles.
   */
  default int getLoadedAssetCount() {
    return 0;
  }

  /**
   * Returns whether the asset at {@code path} is ready (content available without blocking).
   *
   * @param path Asset path.
   * @return {@code true} if the asset is loaded and available.
   */
  boolean isLoaded(@NotNull String path);

  /**
   * Blocks until all queued assets finish loading.
   */
  void finishLoading();

  /**
   * Blocks until the specific asset at {@code path} finishes loading.
   *
   * @param path Asset path.
   */
  void finishLoadingAsset(@NotNull String path);

  /**
   * Unloads the raw content cached for {@code path}, destroying GPU or native resources it
   * holds. The wrapper handle, if any, stays registered and will reload on next use.
   *
   * @param path Asset key to unload.
   */
  void unload(@NotNull String path);

  /**
   * Returns multi-line diagnostics: every cached handle with its path, type, reference count,
   * persist flag, and whether its content is loaded.
   *
   * @return Diagnostic string; never {@code null}.
   */
  @NotNull
  String getDiagnostics();

  /**
   * Unloads non-persistent asset handles whose reference count is zero. Called automatically
   * by {@link org.flixelgdx.Flixel#switchState Flixel.switchState} in
   * {@link FlixelAssetMode#STANDARD} and {@link FlixelAssetMode#AGGRESSIVE} modes.
   */
  void clearNonPersist();

  /**
   * Unloads and removes all cached asset handles, regardless of persist or reference count.
   */
  void clear();

  /**
   * Returns the default {@link FlixelAsset#isPersist()} value assigned to newly created handles.
   *
   * <p>When {@code true}, new handles survive {@link #clearNonPersist()} when unreferenced.
   * Owned assets (e.g. textures created from a {@link org.flixelgdx.graphics.FlixelImage
   * FlixelImage}) always use {@code persist = false} regardless of this setting.
   *
   * @return The global persist default.
   */
  boolean getGlobalPersist();

  /**
   * Sets the global persist default. Does not affect handles already in the cache.
   *
   * @param globalPersist New default value.
   */
  void setGlobalPersist(boolean globalPersist);

  /**
   * Returns the active {@link FlixelAssetMode} controlling when non-persistent assets are reclaimed.
   *
   * @return The current mode; never {@code null}.
   */
  @NotNull
  FlixelAssetMode getAssetMode();

  /**
   * Sets the active asset management mode. Takes effect on the next
   * {@link FlixelAsset#release()} call or the next
   * {@link org.flixelgdx.Flixel#switchState Flixel.switchState}, whichever comes first.
   *
   * @param mode The new mode; must not be {@code null}.
   */
  void setAssetMode(@NotNull FlixelAssetMode mode);

  /**
   * Called by {@link FlixelAsset#release()} when a handle's reference count reaches zero.
   *
   * <p>In {@link FlixelAssetMode#AGGRESSIVE} mode the default implementation triggers an
   * immediate eviction. In other modes this is a no-op; cleanup happens at state-switch time
   * via {@link #clearNonPersist()}.
   *
   * @param handle The handle whose reference count just reached zero.
   */
  default void onAssetReleased(@NotNull FlixelAsset<?> handle) {}

  /**
   * Returns the finished raw content cached for {@code path}, or {@code null} when it has not
   * been loaded yet.
   *
   * <p>This is the storage half of the loader pipeline: wrapper handles such as
   * {@link org.flixelgdx.graphics.FlixelGraphic FlixelGraphic} call it to look up their content
   * (a texture, a string, decoded audio) without knowing how it was produced.
   *
   * @param path Normalized asset path.
   * @return The raw content, or {@code null} when not loaded.
   */
  @Nullable
  Object getRaw(@NotNull String path);

  /**
   * Loads the asset at {@code path} synchronously right now and returns its raw content.
   *
   * <p>This is the mid-frame fallback used by wrapper handles when their content was never
   * queued; it stalls the frame for as long as the load takes. Prefer {@link #load(String)}
   * plus {@link #update()} in a loading state.
   *
   * @param path Normalized asset path.
   * @return The finished raw content; never {@code null}.
   * @throws IllegalArgumentException if no loader is registered for the path's extension.
   */
  @NotNull
  Object loadRawSync(@NotNull String path);

  /**
   * Turns an asset path into the {@link FlixelFile} it will be read from, using the installed
   * file resolver.
   *
   * @param path Normalized asset path.
   * @return The file to read; never {@code null}, though it may not {@link FlixelFile#exists() exist}.
   */
  @NotNull
  FlixelFile resolveFile(@NotNull String path);

  /**
   * Installs the function that turns asset paths into {@link FlixelFile}s.
   *
   * <p>The default resolver reads from {@code Flixel.files.internal(path)}. Games that also run
   * packaged (from a JAR or similar) should install a helper that switches the root type when
   * the on-disk file is missing; see the class Javadoc for an example.
   *
   * @param resolver The resolver to install, or {@code null} to restore the default.
   */
  void setFileResolver(@Nullable FlixelAssetFileResolver resolver);

  /**
   * Resolves {@code path} to the key actually loaded for a texture request, returning a
   * {@code .ktx2} sibling instead of {@code path} when compressed textures are enabled and that
   * sibling exists. Every part of the framework that needs to know whether a texture path has a
   * compressed variant (loading, unloading, checking load state) should go through this method
   * rather than re-implementing the check, so the compressed/plain decision is made in exactly
   * one place. Results are cached, since a path's compressed-sibling status never changes at
   * runtime.
   *
   * @param path Normalized asset path.
   * @return The key to load for this texture.
   * @see #setCompressedTexturesEnabled(boolean)
   */
  @NotNull
  String resolveTexturePath(@NotNull String path);

  /**
   * Turns transparent {@code .ktx2} sibling substitution on or off. Enabled by default on
   * platforms whose graphics backend can transcode compressed textures.
   *
   * @param enabled {@code true} to look for {@code .ktx2} siblings next to texture paths.
   */
  default void setCompressedTexturesEnabled(boolean enabled) {}

  /**
   * Returns whether compressed {@code .ktx2} sibling textures are recognized by this manager.
   *
   * @return {@code true} if {@code .ktx2} siblings are used when present.
   */
  default boolean isCompressedTexturesEnabled() {
    return false;
  }
}
