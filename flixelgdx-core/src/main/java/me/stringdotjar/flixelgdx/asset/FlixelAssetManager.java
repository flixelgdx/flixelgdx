/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.asset;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.utils.Disposable;

import me.stringdotjar.flixelgdx.FlixelDestroyable;

import java.util.function.Function;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Asset manager interface for FlixelGDX.
 *
 * <p>This is the public seam used by sprites and other runtime systems. The default implementation is
 * {@link FlixelDefaultAssetManager}.
 *
 * <p>Prefer {@link #load(FlixelSource)} for loading assets so the asset type is explicit. {@link #load(String)}
 * infers a source from the file extension via the per-manager extension registry; it is convenient but
 * ambiguous if extensions collide or custom content is used—register mappings with
 * {@link #registerExtension(String, Function)} or use {@link #load(FlixelSource)} instead.
 *
 * <p><b>If you ever forget which method to use when it comes to handles, here's a quick reminder:</b>
 * <ul>
 *   <li><b>{@code peek...}</b>: “Is there already a handle for this key?” This should be read only; it may return {@code null}.
 *     It does not create a handle nor does it change the reference count.</li>
 *   <li><b>{@code ensure...}</b>: “Make sure the handle exists.” Get or create the canonical instance and automatically
 *     create a handle if it doesn't exist. The reference count for that said asset handle is unchanged (use when
 *     another layer will {@link FlixelAsset#retain()}, or for inspection).</li>
 *   <li><b>{@code obtain...}</b>: “Give me the handle and count me as a user.” Get or create the asset handle, then
 *     automatically call {@link FlixelAsset#retain()} for {@link FlixelAsset} handles. Call {@link FlixelAsset#release()} when done
 *     (e.g. {@link me.stringdotjar.flixelgdx.FlixelSprite#destroy()} for graphics loaded via {@code obtainWrapper}).</li>
 * </ul>
 */
public interface FlixelAssetManager extends FlixelDestroyable, Disposable {

  /**
   * Loads an asset using a path and the file extension to choose a {@link FlixelSource} factory from the
   * registry on this manager.
   *
   * <p>This is the simplest call for beginners, but it is not always the safest: extension alone may not
   * match how you intend to load the file. For deterministic behavior, prefer {@link #load(FlixelSource)}.
   *
   * @param path Asset path (e.g. internal path like {@code "images/fungus.png"}).
   * @throws IllegalArgumentException if the path has no extension, or no factory is registered for it.
   */
  void load(@NotNull String path);

  /**
   * Registers or replaces the factory used for a file extension (e.g. {@code ".png"} or {@code png}).
   * The factory receives the full path string and returns a {@link FlixelSource} whose runtime type is
   * what libGDX {@link AssetManager} should load (e.g. {@link com.badlogic.gdx.graphics.Texture}).
   *
   * @param extension File extension, with or without a leading dot; normalized case-insensitively.
   * @param factory Produces a source for the given path; must not return {@code null}.
   */
  void registerExtension(@NotNull String extension, @NotNull Function<String, FlixelSource<?>> factory);

  /**
   * Removes a previously registered extension mapping from this manager instance.
   *
   * @param extension Same form as {@link #registerExtension(String, Function)}.
   */
  void unregisterExtension(@NotNull String extension);

  /**
   * Loads by explicit file name and runtime asset type. Prefer {@link #load(FlixelSource)}
   * or {@link #load(String)} unless you need direct control over the libGDX type.
   *
   * @param fileName Asset key/path passed to libGDX.
   * @param type Concrete type registered with {@link com.badlogic.gdx.assets.AssetManager} (e.g. {@link com.badlogic.gdx.graphics.Texture}.class).
   */
  <T> void load(@NotNull String fileName, @NotNull Class<T> type);

  /**
   * Enqueues loading for the given source’s key and runtime type.
   *
   * @param source Non-null source describing what to load.
   */
  void load(@NotNull FlixelSource<?> source);

  /**
   * Enqueues loading using a libGDX asset descriptor.
   *
   * @param assetDescriptor Descriptor for the asset to load.
   */
  void load(@NotNull AssetDescriptor<?> assetDescriptor);

  /**
   * Processes one loading task on the underlying {@link AssetManager}.
   *
   * @return {@code true} when all queued loading is finished.
   */
  boolean update();

  /**
   * Processes loading for up to {@code millis} milliseconds (may yield between steps).
   *
   * @param millis Maximum time to spend updating.
   * @return {@code true} when all queued loading is finished.
   */
  boolean update(int millis);

  /**
   * @return Overall loading progress in {@code [0, 1]} from the underlying {@link AssetManager}.
   */
  float getProgress();

  /**
   * @param fileName Asset file name/key.
   * @return Whether any type of asset with that name is loaded (libGDX {@link AssetManager#isLoaded(String)}).
   */
  boolean isLoaded(@NotNull String fileName);

  /**
   * @param fileName Asset file name/key.
   * @param type Runtime asset type.
   * @return Whether that asset is loaded for the given type.
   */
  boolean isLoaded(@NotNull String fileName, @NotNull Class<?> type);

  /**
   * @param source Source whose key and type are checked.
   * @return Whether the asset described by {@code source} is loaded.
   */
  boolean isLoaded(@NotNull FlixelSource<?> source);

  /**
   * Returns a loaded asset of the given type, or throws if missing.
   *
   * @param fileName Asset key.
   * @param type Runtime type.
   * @param <T> Asset type.
   * @return The loaded instance.
   */
  @NotNull
  <T> T get(@NotNull String fileName, @NotNull Class<T> type);

  /**
   * Returns a loaded asset using the source’s key and type.
   *
   * @param source Non-null source.
   * @param <T> Asset type.
   * @return The loaded instance.
   */
  @NotNull
  <T> T get(@NotNull FlixelSource<T> source);

  /**
   * Requires the source to already be loaded, then returns it (delegates to {@link FlixelSource#require}).
   *
   * @param source Non-null source.
   * @param <T> Asset type.
   * @return The loaded instance.
   */
  @NotNull
  <T> T require(@NotNull FlixelSource<T> source);

  /**
   * Resolves an internal audio path to an absolute filesystem path suitable for native backends (cached).
   *
   * @param path Internal asset path.
   * @return Resolved absolute path string.
   */
  @NotNull
  String resolveAudioPath(@NotNull String path);

  /**
   * Converts an internal path to an absolute filesystem path, extracting to a temp file when needed (e.g. JAR).
   *
   * @param path Internal asset path.
   * @return Absolute path string.
   */
  @NotNull
  String extractAssetPath(@NotNull String path);

  /**
   * Unloads an asset by file name (libGDX {@link AssetManager#unload(String)}) and
   * removes the asset from the GPU VRAM.
   *
   * @param fileName Asset key to unload.
   */
  void unload(@NotNull String fileName);

  /**
   * Blocks until all queued assets are loaded.
   */
  void finishLoading();

  /**
   * Blocks until the asset at {@code fileName} is loaded.
   *
   * @param fileName Asset key.
   */
  void finishLoadingAsset(@NotNull String fileName);

  /**
   * @return Multi-line diagnostics: every asset loaded in the underlying {@link AssetManager} with libGDX
   *   reference counts (load/unload/dependency bookkeeping), plus Flixel {@code retain}/{@code release}
   *   counts where {@link #peekTypedAsset} or texture {@link #peekWrapper} handles exist, then optional
   *   sections for wrapper-only keys (e.g. synthetic owned textures) and typed handles not currently loaded.
   */
  @NotNull
  String getDiagnostics();

  /**
   * Clears non-persistent pooled wrappers and {@link FlixelAsset} handles with zero reference counts.
   */
  void clearNonPersist();

  /**
   * Returns the default {@link FlixelAsset#isPersist()} value assigned to newly created pooled handles
   * ({@link FlixelTypedAsset} and subclasses created through this manager). When {@code true}, new handles
   * stay in the manager cache when their reference count is zero and {@link #clearNonPersist()} runs, so
   * loaded data can remain in memory across state switches until {@link #clear()} or {@link FlixelAsset#setPersist(boolean)}.
   * When {@code false}, new handles may be removed on {@code clearNonPersist()} once unreferenced.
   *
   * <p>Owned wrappers (see {@link me.stringdotjar.flixelgdx.graphics.FlixelGraphic} and {@code isOwned()} on
   * {@link FlixelPooledWrapper}) use {@code persist == false} regardless of this setting so synthetic textures
   * are always eligible for eviction when refcount is zero.
   *
   * <p><b>Owned versus persist</b> (see also {@link me.stringdotjar.flixelgdx.graphics.FlixelGraphic}):
   * <ul>
   *   <li><b>Owned</b> - Structural: this handle wraps a dedicated {@link com.badlogic.gdx.graphics.Texture} that
   *     the framework disposes when the wrapper leaves the pool (for example pixmap or caller-supplied textures).
   *     Not a user toggle; determined by how the graphic was created.</li>
   *   <li><b>Persist</b> - Policy: whether an unreferenced pooled handle is kept in the cache when
   *     {@code clearNonPersist()} runs. Applies to normal path-keyed graphics; owned graphics ignore persist for
   *     that eviction pass so they are always removed at refcount zero.</li>
   * </ul>
   *
   * @return Default persist flag for new typed and path-pooled handles.
   */
  boolean getGlobalPersist();

  /**
   * Sets {@link #getGlobalPersist()}. Does not change {@code persist} on handles already in the cache.
   *
   * @param globalPersist default for future {@link FlixelTypedAsset} and similar creations
   */
  void setGlobalPersist(boolean globalPersist);

  /** Clears all cached assets, regardless if {@link FlixelAsset#isPersist()} is true or not. */
  void clear();

  /**
   * Allocates a unique key for a caller-constructed wrapper (e.g. {@link me.stringdotjar.flixelgdx.graphics.FlixelGraphic}
   * around an owned texture). Use with {@link #registerWrapper(FlixelPooledWrapper)}.
   */
  @NotNull
  String allocateSyntheticWrapperKey();

  /**
   * Registers a caller-built wrapper with the pool for {@link #clearNonPersist()} and keyed lookup.
   * The wrapper’s {@link FlixelPooledWrapper#wrapperRegistrationClass()} must match a factory registered via
   * {@link #registerWrapperFactory(FlixelWrapperFactory)}.
   */
  void registerWrapper(@NotNull FlixelPooledWrapper wrapper);

  /**
   * Registers a {@link FlixelWrapperFactory} for a wrapper type (e.g. built-in
   * {@link me.stringdotjar.flixelgdx.graphics.FlixelGraphicWrapperFactory} for {@link me.stringdotjar.flixelgdx.graphics.FlixelGraphic}).
   * Custom pooled facades use this; loading plain libGDX types uses {@link FlixelSource} + {@link #registerExtension} instead.
   */
  void registerWrapperFactory(@NotNull FlixelWrapperFactory<?> factory);

  /**
   * Returns or creates a pooled wrapper for {@code key} without changing its reference count.
   *
   * <p><b>Beginner shorthand:</b> “Make sure the label exists”. Note this does not claim usage, but the
   * wrapper will be created if it doesn't exist.
   *
   * <p>Use when you need the canonical instance for inspection or to hand off to another API that
   * will call {@link FlixelAsset#retain()} itself. For normal ownership, prefer {@link #obtainWrapper}.
   *
   * @param key Cache key (e.g. asset path for a {@link me.stringdotjar.flixelgdx.graphics.FlixelGraphic}).
   * @param wrapperType Wrapper class registered with {@link #registerWrapperFactory}.
   * @param <W> Wrapper type.
   * @return Pooled wrapper instance.
   */
  @NotNull
  <W> W ensureWrapper(@NotNull String key, @NotNull Class<W> wrapperType);

  /**
   * Returns or creates a pooled wrapper and increments its reference count when it implements {@link FlixelAsset}.
   *
   * <p><b>Beginner shorthand:</b> “Give me the shared handle and count me as a user.” This method automatically calls
   * {@link FlixelAsset#retain()} when executed. Note that you should
   *
   * <p>Equivalent to {@link #ensureWrapper} followed by {@link FlixelAsset#retain()} for {@link FlixelAsset} wrappers.
   * Call {@link FlixelAsset#release()} when done (e.g. from {@link me.stringdotjar.flixelgdx.FlixelSprite#destroy()}).
   *
   * @param key Cache key (e.g. asset path for a {@link me.stringdotjar.flixelgdx.graphics.FlixelGraphic}).
   * @param wrapperType Wrapper class registered by the implementation (e.g. {@link me.stringdotjar.flixelgdx.graphics.FlixelGraphic}.class).
   * @param <W> Wrapper type.
   * @return Pooled wrapper instance.
   */
  @NotNull
  <W> W obtainWrapper(@NotNull String key, @NotNull Class<W> wrapperType);

  /**
   * Returns a pooled wrapper if present, or {@code null}.
   *
   * <p><b>Beginner shorthand:</b> “Is there already a handle?”. This is read only; if the
   * cached wrapper doesn't exist, then it returns {@code null}.
   *
   * @param key Cache key.
   * @param wrapperType Wrapper class.
   * @param <W> Wrapper type.
   * @return Cached wrapper or {@code null}.
   */
  @Nullable
  <W> W peekWrapper(@NotNull String key, @NotNull Class<W> wrapperType);

  /**
   * Returns or creates a typed {@link FlixelAsset} handle without changing its reference count.
   *
   * <p><b>Beginner shorthand:</b> “Make sure the typed handle exists”. Note that this does not automatically call
   * {@link FlixelAsset#retain()}, you still have to manually execute it yourself.
   *
   * <p>For ownership, prefer {@link #obtainTypedAsset} which automatically calls {@link FlixelAsset#retain()}.
   *
   * @param assetKey Asset key.
   * @param type libGDX-loaded asset type.
   * @param <T> Asset type.
   * @return Cached or new handle.
   */
  @NotNull
  <T> FlixelAsset<T> ensureTypedAsset(@NotNull String assetKey, @NotNull Class<T> type);

  /**
   * Returns or creates a typed handle and increments its reference count.
   *
   * <p><b>Beginner shorthand:</b> “Give me the typed handle and count me as a user.” This method automatically calls
   * {@link FlixelAsset#retain()} when executed.
   *
   * <p>Call {@link FlixelAsset#release()} when the owner is done, or use {@link #ensureTypedAsset} for zero-ref access.
   *
   * @param assetKey Asset key.
   * @param type libGDX-loaded asset type.
   * @param <T> Asset type.
   * @return Cached or new handle.
   */
  @NotNull
  <T> FlixelAsset<T> obtainTypedAsset(@NotNull String assetKey, @NotNull Class<T> type);

  /**
   * Peeks at a typed asset handle without creating it.
   *
   * <p><b>Beginner shorthand:</b> “Is there already a typed handle?”—look only; never creates; refcount unchanged.
   *
   * @param assetKey Asset key.
   * @param type Asset type.
   * @return Handle or {@code null}.
   */
  @Nullable
  FlixelAsset<?> peekTypedAsset(@NotNull String assetKey, @NotNull Class<?> type);

  /**
   * Unloads and removes non-persistent typed asset handles with zero reference counts.
   *
   * @param respectPersist If {@link FlixelAsset#isPersist()} should be taken into consideration or should be ignored.
   */
  void clearWrapperAssets(boolean respectPersist);

  /**
   * Unloads and removes non-persistent typed asset handles with zero reference counts.
   *
   * @param respectPersist If {@link FlixelAsset#isPersist()} should be taken into consideration or should be ignored.
   */
  void clearTypedAssets(boolean respectPersist);

  /**
   * @return The underlying libGDX {@link AssetManager} for advanced use (loaders, descriptors, raw APIs).
   */
  @NotNull
  AssetManager getManager();
}
