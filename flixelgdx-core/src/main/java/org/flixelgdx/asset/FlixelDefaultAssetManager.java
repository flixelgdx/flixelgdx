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

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectSet;

import org.flixelgdx.Flixel;
import org.flixelgdx.audio.FlixelSoundManager;
import org.flixelgdx.audio.FlixelSoundSource;
import org.flixelgdx.audio.FlixelSoundSourceLoader;
import org.flixelgdx.graphics.FlixelGraphic;
import org.flixelgdx.util.FlixelString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Objects;

/**
 * Default concrete asset manager for FlixelGDX.
 *
 * <p>Maintains a single {@code ObjectMap<String, FlixelAsset<?>>} cache as the source of truth.
 * The underlying libGDX {@link AssetManager} is used only for async I/O; once a raw asset is
 * available, game code works exclusively with the typed {@link FlixelAsset} handles returned by
 * {@link #get(String)}.
 *
 * <p>Loaders are registered per file extension. The constructor registers defaults for images
 * ({@code .png}, {@code .jpg}, {@code .jpeg}, {@code .webp}), audio ({@code .mp3}, {@code .ogg},
 * {@code .wav}, {@code .flac}), and text ({@code .txt}, {@code .xml}, {@code .json}). Add custom
 * extensions via {@link #registerLoader(String, Class, FlixelAssetLoader)}.
 *
 * <p><b>Compressed textures:</b> {@link org.flixelgdx.FlixelGame#create() FlixelGame.create()}
 * calls {@link #enableCompressedTextures()} automatically on every backend so {@code .png}
 * requests transparently prefer a {@code .ktx2} sibling when one exists.
 *
 * <p><b>Recommended usage:</b> Access via {@link org.flixelgdx.Flixel#assets Flixel.assets}.
 *
 * <p><b>Web audio pre-decoding:</b> any {@link #load} call for an audio path automatically
 * triggers background audio decoding on the web platform
 * ({@link Application.ApplicationType#WebGL}). By the time the loading state finishes the
 * decoded buffer is cached and
 * {@link FlixelSoundManager#play FlixelSoundManager.play} returns instantly.
 * On desktop and Android this is a no-op.
 *
 * <pre>{@code
 * // Inside of a loading state.
 * Flixel.assets.load("music/inst.mp3");
 * Flixel.assets.load("music/voices.mp3");
 * Flixel.assets.finishLoading();
 *
 * // Game state, with buffers already decoded.
 * Flixel.sound.play("music/inst.mp3");
 * Flixel.sound.play("music/voices.mp3");
 * }</pre>
 *
 * <p><b>Experts:</b> {@link #getManager()} exposes the underlying {@link AssetManager} for
 * custom loaders, asset descriptors, or raw APIs not wrapped here.
 */
public class FlixelDefaultAssetManager implements FlixelAssetManager {

  private AssetManager manager;

  private final ObjectMap<String, FlixelAsset<?>> cache = new ObjectMap<>();
  private final ObjectMap<String, LoaderEntry<?>> loaderRegistry = new ObjectMap<>();
  private final ObjectMap<String, String> audioPathCache = new ObjectMap<>();
  private final ObjectMap<String, String> texturePathCache = new ObjectMap<>();
  private final ObjectSet<String> pendingPersistKeys = new ObjectSet<>();
  private final FlixelString diagnosticsString = new FlixelString();
  private FlixelAssetMode assetMode = FlixelAssetMode.STANDARD;
  private int syntheticKeyId;

  private boolean globalPersist = false;
  private boolean compressedTexturesEnabled;

  /** Platform-provided installer for the KTX2 texture loader; {@code null} where unsupported. */
  @Nullable
  private static FlixelKtx2LoaderInstaller ktx2LoaderInstaller;

  /** Constructs a new manager with default loaders for images, audio, and text. */
  public FlixelDefaultAssetManager() {
    manager = new AssetManager();
    manager.setLoader(String.class, new FlixelStringAssetLoader(manager.getFileHandleResolver()));
    manager.setLoader(
        FlixelSoundSource.class,
        new FlixelSoundSourceLoader(manager.getFileHandleResolver()));

    FlixelAssetLoader<FlixelGraphic> graphicLoader = FlixelGraphic::new;
    registerLoader(".png", Texture.class, graphicLoader);
    registerLoader(".jpg", Texture.class, graphicLoader);
    registerLoader(".jpeg", Texture.class, graphicLoader);
    registerLoader(".webp", Texture.class, graphicLoader);

    FlixelAssetLoader<FlixelSoundSource> soundLoader =
        (assets, path) -> new FlixelDefaultAsset<>(assets, path, FlixelSoundSource.class);
    registerLoader(".mp3", FlixelSoundSource.class, soundLoader);
    registerLoader(".ogg", FlixelSoundSource.class, soundLoader);
    registerLoader(".wav", FlixelSoundSource.class, soundLoader);
    registerLoader(".flac", FlixelSoundSource.class, soundLoader);

    FlixelAssetLoader<String> textLoader =
        (assets, path) -> new FlixelDefaultAsset<>(assets, path, String.class);
    registerLoader(".txt", String.class, textLoader);
    registerLoader(".xml", String.class, textLoader);
    registerLoader(".json", String.class, textLoader);
  }

  @Override
  public void load(@NotNull String path) {
    path = requireNormalized(path);
    LoaderEntry<?> entry = requireEntry(path);
    manager.load(resolveTexturePath(path), entry.rawType);
    if (entry.rawType == FlixelSoundSource.class) {
      prewarmAudio(path);
    }
  }

  @Override
  public void load(@NotNull String path, boolean persist) {
    path = requireNormalized(path);
    if (persist) {
      pendingPersistKeys.add(path);
    }
    LoaderEntry<?> entry = requireEntry(path);
    manager.load(resolveTexturePath(path), entry.rawType);
    if (entry.rawType == FlixelSoundSource.class) {
      prewarmAudio(path);
    }
  }

  @NotNull
  @Override
  @SuppressWarnings("unchecked")
  public <T> FlixelAsset<T> get(@NotNull String path) {
    Objects.requireNonNull(path, "path cannot be null.");
    path = FlixelAssetPaths.normalizeAssetPath(path);
    if (path.isEmpty()) {
      throw new IllegalArgumentException("path cannot be empty.");
    }
    FlixelAsset<?> cached = cache.get(path);
    if (cached != null) {
      return (FlixelAsset<T>) cached;
    }
    LoaderEntry<?> entry = requireEntry(path);
    FlixelAsset<T> handle = ((LoaderEntry<T>) entry).loader.create(this, path);
    applyPendingPersist(path, handle);
    cache.put(path, handle);
    return handle;
  }

  @Nullable
  @Override
  public FlixelAsset<?> peek(@NotNull String path) {
    Objects.requireNonNull(path, "path cannot be null.");
    return cache.get(FlixelAssetPaths.normalizeAssetPath(path));
  }

  @Override
  public <T> void registerLoader(
      @NotNull String extension,
      @NotNull Class<?> rawType,
      @NotNull FlixelAssetLoader<T> loader) {
    Objects.requireNonNull(extension, "extension cannot be null.");
    Objects.requireNonNull(rawType, "rawType cannot be null.");
    Objects.requireNonNull(loader, "loader cannot be null.");
    loaderRegistry.put(normalizeExtension(extension), new LoaderEntry<>(rawType, loader));
  }

  @Override
  public void unregisterLoader(@NotNull String extension) {
    Objects.requireNonNull(extension, "extension cannot be null.");
    loaderRegistry.remove(normalizeExtension(extension));
  }

  @Override
  public void register(@NotNull FlixelAsset<?> asset) {
    Objects.requireNonNull(asset, "asset cannot be null.");
    cache.put(asset.getPath(), asset);
  }

  @NotNull
  @Override
  public String allocateSyntheticKey() {
    return "__flixel_syn__/" + (syntheticKeyId++);
  }

  @NotNull
  @Override
  public AssetManager getManager() {
    return manager;
  }

  @Override
  public void enableCompressedTextures() {
    if (compressedTexturesEnabled) {
      return;
    }
    if (ktx2LoaderInstaller == null) {
      return;
    }
    ktx2LoaderInstaller.install(manager);
  }

  @Override
  public boolean isCompressedTexturesEnabled() {
    return compressedTexturesEnabled;
  }

  @Override
  public void setKtx2LoaderInstaller(@Nullable FlixelKtx2LoaderInstaller installer) {
    ktx2LoaderInstaller = installer;
  }

  @Override
  public boolean update() {
    boolean prewarmPending = Flixel.getSoundFactory() != null && !Flixel.getSoundFactory().isPrewarmPending();
    return manager.update() && !prewarmPending;
  }

  @Override
  public boolean update(int millis) {
    boolean prewarmPending = Flixel.getSoundFactory() != null && !Flixel.getSoundFactory().isPrewarmPending();
    return manager.update(millis) && prewarmPending;
  }

  @Override
  public float getProgress() {
    return manager.getProgress();
  }

  @Override
  public int getLoadedAssetCount() {
    return cache.size;
  }

  @Override
  public boolean isLoaded(@NotNull String path) {
    Objects.requireNonNull(path, "path cannot be null.");
    path = FlixelAssetPaths.normalizeAssetPath(path);
    FlixelAsset<?> cached = cache.get(path);
    if (cached != null) {
      return cached.isLoaded();
    }
    LoaderEntry<?> entry = loaderRegistry.get(extensionOf(path));
    if (entry == null) {
      return false;
    }
    return manager.isLoaded(resolveTexturePath(path), entry.rawType);
  }

  @Override
  public void finishLoading() {
    manager.finishLoading();
  }

  @Override
  public void finishLoadingAsset(@NotNull String path) {
    manager.finishLoadingAsset(
        resolveTexturePath(
            FlixelAssetPaths.normalizeAssetPath(
                Objects.requireNonNull(path, "path cannot be null."))));
  }

  @Override
  public void unload(@NotNull String path) {
    manager.unload(
        resolveTexturePath(
            FlixelAssetPaths.normalizeAssetPath(
                Objects.requireNonNull(path, "path cannot be null."))));
  }

  @NotNull
  @Override
  public String getDiagnostics() {
    diagnosticsString.clear();
    if (manager == null) {
      diagnosticsString.concat("(asset manager disposed)\n");
      return diagnosticsString.toString();
    }
    diagnosticsString.concat("Mode: ").concat(assetMode.name()).concat("\n");
    diagnosticsString.concat("------------------------- ASSET CACHE -------------------------\n");
    for (ObjectMap.Entry<String, FlixelAsset<?>> e : cache) {
      FlixelAsset<?> asset = e.value;
      diagnosticsString
          .concat("\tKey: ")
          .concat(e.key)
          .concat(", Type: ")
          .concat(asset.getClass().getSimpleName())
          .concat(", Refs: ")
          .concat(asset.getRefCount())
          .concat(", Persist: ")
          .concat(asset.isPersist())
          .concat(", Loaded: ")
          .concat(asset.isLoaded())
          .concat("\n");
    }
    return diagnosticsString.toString();
  }

  @Override
  public boolean getGlobalPersist() {
    return globalPersist;
  }

  @Override
  public void setGlobalPersist(boolean globalPersist) {
    this.globalPersist = globalPersist;
  }

  @NotNull
  @Override
  public FlixelAssetMode getAssetMode() {
    return assetMode;
  }

  @Override
  public void setAssetMode(@NotNull FlixelAssetMode mode) {
    this.assetMode = Objects.requireNonNull(mode, "mode cannot be null.");
  }

  @Override
  public void onAssetReleased(@NotNull FlixelAsset<?> handle) {
    if (assetMode != FlixelAssetMode.AGGRESSIVE) {
      return;
    }
    if (handle.isPersist()) {
      return;
    }
    evict(handle.getPath(), handle);
    cache.remove(handle.getPath());
  }

  @Override
  public void clearNonPersist() {
    Array<String> toRemove = null;
    for (ObjectMap.Entry<String, FlixelAsset<?>> e : cache) {
      FlixelAsset<?> asset = e.value;
      if (asset.getRefCount() > 0 || asset.isPersist()) {
        continue;
      }
      evict(e.key, asset);
      if (toRemove == null) {
        toRemove = new Array<>();
      }
      toRemove.add(e.key);
    }
    if (toRemove != null) {
      for (int i = 0; i < toRemove.size; i++) {
        cache.remove(toRemove.get(i));
      }
    }
  }

  @Override
  public void clear() {
    for (ObjectMap.Entry<String, FlixelAsset<?>> e : cache) {
      evict(e.key, e.value);
    }
    cache.clear();
  }

  @Override
  public void destroy() {
    if (manager != null) {
      manager.dispose();
      manager = null;
    }
    cache.clear();
    loaderRegistry.clear();
    audioPathCache.clear();
    texturePathCache.clear();
    pendingPersistKeys.clear();
    syntheticKeyId = 0;
    assetMode = FlixelAssetMode.STANDARD;
  }

  @Override
  public void dispose() {
    destroy();
  }

  @NotNull
  @Override
  public String resolveAudioPath(@NotNull String path) {
    path = FlixelAssetPaths.normalizeAssetPath(Objects.requireNonNull(path, "path cannot be null."));
    String cached = audioPathCache.get(path);
    if (cached != null) {
      return cached;
    }
    String resolved = extractAssetPath(path);
    audioPathCache.put(path, resolved);
    return resolved;
  }

  @NotNull
  @Override
  public String extractAssetPath(@NotNull String path) {
    path = FlixelAssetPaths.normalizeAssetPath(Objects.requireNonNull(path, "path cannot be null."));
    // On platforms other than desktop, there's typically no real file system we can
    // extract an asset to, so we simply return the original path that was provided.
    var hasRealFileSystem = Gdx.app != null && Gdx.app.getType() == Application.ApplicationType.Desktop;
    if (!hasRealFileSystem) {
      return path;
    }
    FileHandle handle = Gdx.files.internal(path);
    try {
      File file = handle.file();
      if (file.exists()) {
        return file.getAbsolutePath();
      }
    } catch (Exception ignored) {
      // The asset is inside a packaged JAR, meaning there's no real file system path to be resolved.
    }
    String ext = path.contains(".") ? path.substring(path.lastIndexOf('.')) : ".tmp";
    try {
      File temp = File.createTempFile("flixel_asset_", ext);
      temp.deleteOnExit();
      handle.copyTo(new FileHandle(temp));
      return temp.getAbsolutePath();
    } catch (IOException e) {
      throw new RuntimeException("Failed to extract asset from JAR: " + path, e);
    }
  }

  @NotNull
  @Override
  public String resolveTexturePath(@NotNull String path) {
    if (!compressedTexturesEnabled) {
      return path;
    }
    String cached = texturePathCache.get(path);
    if (cached != null) {
      return cached;
    }
    String resolved = FlixelAssetPaths.resolveCompressedTexturePath(path);
    texturePathCache.put(path, resolved);
    return resolved;
  }

  private void evict(@NotNull String path, @NotNull FlixelAsset<?> asset) {
    if (asset instanceof FlixelGraphic g) {
      if (g.isOwned()) {
        Texture t = g.getOwnedTexture();
        if (t != null) {
          t.dispose();
        }
      } else if (manager != null && manager.isLoaded(g.getResolvedPath(), Texture.class)) {
        manager.unload(g.getResolvedPath());
      }
    } else if (asset instanceof FlixelDefaultAsset<?> da) {
      if (manager != null && manager.isLoaded(path, da.getRawType())) {
        manager.unload(path);
      }
    }
  }

  private void applyPendingPersist(@NotNull String path, @NotNull FlixelAsset<?> handle) {
    if (pendingPersistKeys.remove(path)) {
      handle.setPersist(true);
    }
  }

  @NotNull
  private String requireNormalized(@NotNull String path) {
    Objects.requireNonNull(path, "path cannot be null.");
    path = FlixelAssetPaths.normalizeAssetPath(path);
    if (path.isEmpty()) {
      throw new IllegalArgumentException("path cannot be null/empty.");
    }
    return path;
  }

  @NotNull
  private LoaderEntry<?> requireEntry(@NotNull String path) {
    String ext = extensionOf(path);
    if (ext.isEmpty()) {
      throw new IllegalArgumentException(
          "Cannot infer asset type from path (no extension): \""
              + path
              + "\". Use registerLoader(...) for extensionless paths.");
    }
    LoaderEntry<?> entry = loaderRegistry.get(ext);
    if (entry == null) {
      throw new IllegalArgumentException(
          "No loader registered for extension \""
              + ext
              + "\" (path: \""
              + path
              + "\"). Call registerLoader(\""
              + ext
              + "\", ...) first.");
    }
    return entry;
  }

  @NotNull
  private static String extensionOf(@NotNull String path) {
    int lastSlash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
    String name = lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    int dot = name.lastIndexOf('.');
    if (dot < 0 || dot == name.length() - 1) {
      return "";
    }
    return name.substring(dot).toLowerCase(Locale.ROOT);
  }

  @NotNull
  private static String normalizeExtension(@NotNull String ext) {
    ext = ext.trim().toLowerCase(Locale.ROOT);
    if (ext.isEmpty()) {
      throw new IllegalArgumentException("extension cannot be empty.");
    }
    if (!ext.startsWith(".")) {
      ext = "." + ext;
    }
    return ext;
  }

  private void prewarmAudio(@NotNull String path) {
    if (Gdx.app == null || Gdx.app.getType() != Application.ApplicationType.WebGL) {
      return;
    }
    if (Flixel.getSoundFactory() != null) {
      Flixel.getSoundFactory().prewarmSound(resolveAudioPath(path));
    }
  }

  /** Pairs a libGDX raw type with its {@link FlixelAssetLoader}. */
  private record LoaderEntry<T>(@NotNull Class<?> rawType, @NotNull FlixelAssetLoader<T> loader) {
  }
}
