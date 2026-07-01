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

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.utils.Disposable;

import org.flixelgdx.functional.FlixelDestroyable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Asset manager interface for FlixelGDX.
 *
 * <p>This is the public seam used by sprites and other runtime systems. The default
 * implementation is {@link FlixelDefaultAssetManager}. Access via
 * {@link org.flixelgdx.Flixel#assets Flixel.assets}.
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
 * per-manager loader registry ({@link #registerLoader}). Prefer it over explicit type
 * parameters for common asset types. Use {@link #registerLoader} to add custom extensions.
 *
 * <p>Path keys are normalized (collapsed slashes, unified separators) via
 * {@link FlixelAssetPaths#normalizeAssetPath(String)} so duplicate slashes or backslashes
 * do not cause mismatches on web builds or other backends where paths are compared literally.
 *
 * <p><b>For advanced libGDX interop</b> (custom loaders, asset descriptors, raw API access),
 * use {@link #getManager()} to reach the underlying {@link AssetManager} directly.
 */
public interface FlixelAssetManager extends FlixelDestroyable, Disposable {

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
   * Registers a loader for a file extension. The {@code rawType} is what libGDX's
   * {@link AssetManager} will load under the hood (e.g. {@link com.badlogic.gdx.graphics.Texture}
   * for image files). The {@code loader} wraps the path into a {@link FlixelAsset} handle.
   *
   * <p>Example: adding a custom config extension that loads text:
   *
   * <pre>{@code
   * Flixel.assets.registerLoader(".cfg", String.class,
   *     (assets, path) -> new FlixelDefaultAsset<>(assets, path, String.class));
   * }</pre>
   *
   * @param extension File extension with or without a leading dot (e.g. {@code ".png"} or {@code "png"}).
   * @param rawType The libGDX raw type to load.
   * @param loader Creates handles for paths with this extension.
   * @param <T> The wrapper type the loader produces.
   */
  <T> void registerLoader(
      @NotNull String extension,
      @NotNull Class<?> rawType,
      @NotNull FlixelAssetLoader<T> loader);

  /**
   * Removes a previously registered loader for the given extension.
   *
   * @param extension Same form as {@link #registerLoader}.
   */
  void unregisterLoader(@NotNull String extension);

  /**
   * Registers a caller-constructed asset handle directly with the manager cache. Use this for
   * assets created outside the normal loading pipeline (e.g. a texture built from a
   * {@link com.badlogic.gdx.graphics.Pixmap}).
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
   * Processes one loading task on the underlying {@link AssetManager}.
   *
   * @return {@code true} when all queued loading is finished.
   */
  boolean update();

  /**
   * Processes loading for up to {@code millis} milliseconds.
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
   * Unloads the asset at {@code path} from the underlying libGDX {@link AssetManager}.
   *
   * @param path Asset key to unload.
   */
  void unload(@NotNull String path);

  /**
   * Returns multi-line diagnostics: every cached handle with its path, type, reference count,
   * persist flag, and whether the underlying libGDX asset is loaded.
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
   * Owned assets (e.g. textures created from a {@link com.badlogic.gdx.graphics.Pixmap}) always
   * use {@code persist = false} regardless of this setting.
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
   * Resolves an internal audio path to an absolute filesystem path suitable for native backends.
   * Results are cached after the first call.
   *
   * @param path Internal asset path.
   * @return Resolved absolute path; never {@code null}.
   */
  @NotNull
  String resolveAudioPath(@NotNull String path);

  /**
   * Converts an internal path to an absolute filesystem path, extracting to a temp file when the
   * asset lives inside a JAR.
   *
   * @param path Internal asset path.
   * @return Absolute path; never {@code null}.
   */
  @NotNull
  String extractAssetPath(@NotNull String path);

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
   * Returns the underlying libGDX {@link AssetManager} for advanced use such as custom loaders,
   * asset descriptors, or raw API access not covered by this interface.
   *
   * @return The underlying manager; never {@code null}.
   */
  @NotNull
  AssetManager getManager();

  /**
   * Registers a KTX2/Basis Universal texture loader so {@code .png} requests transparently use a
   * compressed {@code .ktx2} sibling when one exists next to the requested path.
   *
   * <p>Call this once during platform-specific startup, before loading any graphics, on backends
   * that ship the Basis Universal transcoder natives (the Android backend calls this
   * automatically during launch). Calling it on a backend without those natives available is
   * harmless but pointless, since {@link org.flixelgdx.graphics.FlixelGraphic FlixelGraphic} only
   * looks for a {@code .ktx2} sibling after this has been called, and no {@code .ktx2} files exist
   * unless the {@code org.flixelgdx.basisu} Gradle plugin compressed them into the build.
   * Idempotent: calling it more than once has no additional effect.
   *
   * @see #isCompressedTexturesEnabled()
   */
  void enableCompressedTextures();

  /**
   * Returns whether {@link #enableCompressedTextures()} has been called on this manager.
   *
   * @return {@code true} if compressed {@code .ktx2} textures are recognized by this manager.
   */
  boolean isCompressedTexturesEnabled();
}
