/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.asset;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import com.badlogic.gdx.graphics.Texture;

import me.stringdotjar.flixelgdx.audio.FlixelSoundSource;
import me.stringdotjar.flixelgdx.audio.FlixelSoundSourceLoader;
import me.stringdotjar.flixelgdx.graphics.FlixelGraphic;
import me.stringdotjar.flixelgdx.graphics.FlixelGraphicSource;
import me.stringdotjar.flixelgdx.graphics.FlixelGraphicWrapperFactory;

import me.stringdotjar.flixelgdx.util.FlixelString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Default concrete asset manager for FlixelGDX.
 *
 * <p>This class centralizes asset loading and lifecycle policy in one place. It wraps a single libGDX
 * {@link AssetManager}, plus pooled wrappers (via {@link FlixelWrapperFactory}) and {@link FlixelTypedAsset}
 * that track {@code persist} and reference counts and can be cleared on state switches.
 *
 * <p><b>Recommended usage:</b> Access this via {@link me.stringdotjar.flixelgdx.Flixel#assets}.
 *
 * <p><b>Path loading:</b> {@link #load(String)} infers a {@link FlixelSource} from the file extension using
 * {@link #registerExtension(String, Function)}. Prefer {@link #load(FlixelSource)} when the asset type must be explicit.
 *
 * <p><b>Experts:</b> {@link #getManager()} exposes the underlying {@link AssetManager} for custom
 * loaders, {@link AssetDescriptor} batches, or APIs not wrapped here.
 */
public class FlixelDefaultAssetManager implements FlixelAssetManager {

  private AssetManager manager;

  private final ConcurrentHashMap<String, String> audioPathCache = new ConcurrentHashMap<>();

  private final ObjectMap<Class<?>, FlixelWrapperFactory<?>> wrapperFactories = new ObjectMap<>();
  private int syntheticWrapperId;

  private final ObjectMap<AssetId, FlixelTypedAsset<?>> typedAssetCache = new ObjectMap<>();

  /** Per-instance extension (e.g. {@code .png}) to source factory for {@link #load(String)}. */
  private final ConcurrentHashMap<String, Function<String, FlixelSource<?>>> extensionRegistry = new ConcurrentHashMap<>();

  /** Default {@link FlixelTypedAsset#isPersist()} for handles created after construction; see {@link #getGlobalPersist()}. */
  private boolean globalPersist = false;

  /** FlixelString object to prevent allocation every time {@link #getDiagnostics()} is called. */
  private final FlixelString diagnosticsString = new FlixelString();

  private record AssetId(@NotNull String key, @NotNull Class<?> type) {
    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof AssetId other)) return false;
      return key.equals(other.key) && type.equals(other.type);
    }

    @Override
    public int hashCode() {
      return 31 * key.hashCode() + type.hashCode();
    }
  }

  /** Constructs a new asset manager with the default loaders for strings and sound sources. */
  public FlixelDefaultAssetManager() {
    manager = new AssetManager();
    manager.setLoader(String.class, new FlixelStringAssetLoader(manager.getFileHandleResolver()));
    manager.setLoader(FlixelSoundSource.class, new FlixelSoundSourceLoader(manager.getFileHandleResolver()));

    // Register the default extensions for loading assets with just their path.
    Function<String, FlixelSource<?>> graphic = FlixelGraphicSource::new;
    registerExtension(".png", graphic);
    registerExtension(".jpg", graphic);
    registerExtension(".jpeg", graphic);
    registerExtension(".webp", graphic);

    Function<String, FlixelSource<?>> sound = FlixelSoundSource::new;
    registerExtension(".mp3", sound);
    registerExtension(".ogg", sound);
    registerExtension(".wav", sound);
    registerExtension(".flac", sound);

    Function<String, FlixelSource<?>> text = FlixelStringAssetSource::new;
    registerExtension(".txt", text);
    registerExtension(".xml", text);
    registerExtension(".json", text);

    registerWrapperFactory(new FlixelGraphicWrapperFactory());
  }

  @NotNull
  private static String normalizeExtension(@NotNull String extension) {
    String e = extension.trim().toLowerCase(Locale.ROOT);
    if (e.isEmpty()) {
      throw new IllegalArgumentException("extension cannot be empty.");
    }
    if (!e.startsWith(".")) {
      e = "." + e;
    }
    return e;
  }

  /**
   * Returns the last path segment's extension including the dot (e.g. {@code .png}), or empty if none.
   */
  @NotNull
  private static String fileExtensionFromPath(@NotNull String path) {
    int lastSlash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
    String name = lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    int dot = name.lastIndexOf('.');
    if (dot < 0 || dot == name.length() - 1) {
      return "";
    }
    return name.substring(dot).toLowerCase(Locale.ROOT);
  }

  @Override
  public void load(@NotNull String path) {
    if (path == null || path.isEmpty()) {
      throw new IllegalArgumentException("path cannot be null/empty.");
    }
    String ext = fileExtensionFromPath(path);
    if (ext.isEmpty()) {
      throw new IllegalArgumentException(
        "Cannot infer asset type from path (no extension): \"" + path + "\". "
          + "Use load(FlixelSource) or registerExtension(...) for extensionless paths."
      );
    }
    Function<String, FlixelSource<?>> factory = extensionRegistry.get(ext);
    if (factory == null) {
      throw new IllegalArgumentException(
        "No source factory registered for extension \"" + ext + "\" (path: \"" + path + "\"). "
          + "Call registerExtension(\"" + ext + "\", factory) or use load(FlixelSource) explicitly."
      );
    }
    FlixelSource<?> source = factory.apply(path);
    if (source == null) {
      throw new IllegalStateException("Extension factory for \"" + ext + "\" returned null for path \"" + path + "\".");
    }
    load(source);
  }

  @Override
  public <T> void load(@NotNull String fileName, @NotNull Class<T> type) {
    manager.load(fileName, type);
  }

  @Override
  public void load(@NotNull FlixelSource<?> source) {
    Objects.requireNonNull(source, "source cannot be null.");
    load(source.getAssetKey(), source.getType());
  }

  @Override
  public void load(@NotNull AssetDescriptor<?> assetDescriptor) {
    manager.load(assetDescriptor);
  }

  @Override
  public void registerExtension(@NotNull String extension, @NotNull Function<String, FlixelSource<?>> factory) {
    if (extension == null) {
      throw new IllegalArgumentException("extension cannot be null.");
    }
    if (factory == null) {
      throw new IllegalArgumentException("factory cannot be null.");
    }
    extensionRegistry.put(normalizeExtension(extension), factory);
  }

  @Override
  public void unregisterExtension(@NotNull String extension) {
    if (extension == null) {
      throw new IllegalArgumentException("extension cannot be null.");
    }
    extensionRegistry.remove(normalizeExtension(extension));
  }

  /** Returns the underlying libGDX {@link AssetManager}. */
  @NotNull
  @Override
  public AssetManager getManager() {
    return manager;
  }

  @Override
  public boolean update() {
    return manager.update();
  }

  @Override
  public boolean update(int millis) {
    return manager.update(millis);
  }

  @Override
  public float getProgress() {
    return manager.getProgress();
  }

  @Override
  public boolean isLoaded(@NotNull String fileName) {
    return manager.isLoaded(fileName);
  }

  @Override
  public boolean isLoaded(@NotNull String fileName, @NotNull Class<?> type) {
    return manager.isLoaded(fileName, type);
  }

  @Override
  public boolean isLoaded(@NotNull FlixelSource<?> source) {
    if (source == null) {
      throw new IllegalArgumentException("Source cannot be null.");
    }
    return isLoaded(source.getAssetKey(), source.getType());
  }

  @NotNull
  @Override
  public <T> T get(@NotNull String fileName, @NotNull Class<T> type) {
    return manager.get(fileName, type);
  }

  @NotNull
  @Override
  public <T> T get(@NotNull FlixelSource<T> source) {
    if (source == null) {
      throw new IllegalArgumentException("Source cannot be null.");
    }
    return get(source.getAssetKey(), source.getType());
  }

  @NotNull
  @Override
  public <T> T require(@NotNull FlixelSource<T> source) {
    if (source == null) {
      throw new IllegalArgumentException("Source cannot be null.");
    }
    return source.require(this);
  }

  @NotNull
  @Override
  public String resolveAudioPath(@NotNull String path) {
    return audioPathCache.computeIfAbsent(path, this::extractAssetPath);
  }

  @NotNull
  @Override
  public String extractAssetPath(@NotNull String path) {
    // On web/TeaVM, audio is loaded via Gdx.files.internal() which reads from
    // the virtual filesystem populated by the preloader, which does not require filesystem extraction.
    // This is because browsers don't actually have a real filesystem, so we simply return the path as is.
    if (Gdx.app != null && Gdx.app.getType() == Application.ApplicationType.WebGL) {
      return path;
    }

    FileHandle handle = Gdx.files.internal(path);
    try {
      File file = handle.file();
      if (file.exists()) {
        return file.getAbsolutePath();
      }
    } catch (Exception ignored) {
      // When running from a packaged JAR, internal/classpath handles may not expose a real filesystem File.
    }
    String ext = path.contains(".") ? path.substring(path.lastIndexOf('.')) : "";
    try {
      if (ext.isEmpty()) {
        ext = ".tmp";
      }
      File temp = File.createTempFile("flixel_asset_", ext);
      temp.deleteOnExit();
      handle.copyTo(new FileHandle(temp));
      return temp.getAbsolutePath();
    } catch (IOException e) {
      throw new RuntimeException("Failed to extract asset from JAR: " + path, e);
    }
  }

  @Override
  public void unload(@NotNull String fileName) {
    manager.unload(fileName);
  }

  @Override
  public void finishLoading() {
    manager.finishLoading();
  }

  @Override
  public void finishLoadingAsset(@NotNull String fileName) {
    manager.finishLoadingAsset(fileName);
  }

  @NotNull
  @Override
  public String getDiagnostics() {
    diagnosticsString.clear();
    if (manager == null) {
      diagnosticsString.concat("(asset manager disposed)\n");
      return diagnosticsString.toString();
    }

    // Output loaded assets in the libGDX AssetManager.
    diagnosticsString.concat("------------------------- LOADED ASSETS -------------------------\n");
    Array<String> assetNames = manager.getAssetNames();
    Set<String> managerKeys = new HashSet<>(Math.max(16, assetNames.size * 2));
    synchronized (manager) {
      for (int i = 0; i < assetNames.size; i++) {
        String fileName = assetNames.get(i);
        managerKeys.add(fileName);
        Class<?> type = manager.getAssetType(fileName);
        int libRefs = manager.getReferenceCount(fileName);
        int flixelRefs = type != null ? flixelRefCountForLoadedAsset(fileName, type) : 0;
        diagnosticsString
          .concat("\tKey: ")
          .concat(fileName)
          .concat(", Type: ")
          .concat(type != null ? type.getName() : "?")
          .concat(", Flixel refs: ")
          .concat(flixelRefs)
          .concat(", libGDX refs: ")
          .concat(libRefs);
        Array<String> deps = manager.getDependencies(fileName);
        if (deps != null && deps.size > 0) {
          diagnosticsString.concat(", deps: [");
          for (int d = 0; d < deps.size; d++) {
            if (d > 0) {
              diagnosticsString.concat(", ");
            }
            diagnosticsString.concat(deps.get(d));
          }
          diagnosticsString.concat("]");
        }
        diagnosticsString.concat("\n");
      }
    }

    // Output wrapper-only assets that are not loaded in the libGDX AssetManager.
    diagnosticsString.concat("------------------------- WRAPPER-ONLY -------------------------\n");
    for (ObjectMap.Entry<Class<?>, FlixelWrapperFactory<?>> entry : wrapperFactories.iterator()) {
      FlixelWrapperFactory<?> factory = entry.value;
      factory.forEachWrappedAsset(asset -> {
        if (!(asset instanceof FlixelTypedAsset<?> a)) {
          return;
        }
        if (managerKeys.contains(a.getAssetKey())) {
          return;
        }
        diagnosticsString
          .concat("\tKey: ")
          .concat(a.getAssetKey())
          .concat(", Type: ")
          .concat(a.getType().getName())
          .concat(", Flixel refs: ")
          .concat(a.getRefCount())
          .concat(", libGDX refs: n/a")
          .concat("\n");
      });
    }

    // Output typed assets that are not loaded in the libGDX AssetManager.
    diagnosticsString.concat("------------------------- TYPED HANDLES -------------------------\n");
    for (FlixelTypedAsset<?> asset : typedAssetCache.values()) {
      if (manager != null && manager.isLoaded(asset.getAssetKey(), asset.getType())) {
        continue;
      }
      diagnosticsString
        .concat("\tKey: ")
        .concat(asset.getAssetKey())
        .concat(", Type: ")
        .concat(asset.getType().getName())
        .concat(", Flixel refs: ")
        .concat(asset.getRefCount())
        .concat(", libGDX refs: n/a")
        .concat("\n");
    }

    return diagnosticsString.toString();
  }

  /**
   * Flixel {@code retain}/{@code release} count for a key already loaded in {@link AssetManager}: explicit
   * {@link FlixelTypedAsset} if present, else {@link FlixelGraphic} for textures.
   */
  private int flixelRefCountForLoadedAsset(@NotNull String fileName, @NotNull Class<?> type) {
    FlixelAsset<?> typed = peekTypedAsset(fileName, type);
    if (typed != null) {
      return typed.getRefCount();
    }
    if (type == Texture.class) {
      FlixelGraphic g = peekWrapper(fileName, FlixelGraphic.class);
      return g != null ? g.getRefCount() : 0;
    }
    return 0;
  }

  @Override
  public boolean getGlobalPersist() {
    return globalPersist;
  }

  @Override
  public void setGlobalPersist(boolean globalPersist) {
    this.globalPersist = globalPersist;
  }

  @Override
  public void clearNonPersist() {
    clearWrapperAssets(true);
    clearTypedAssets(true);
  }

  @Override
  public void clear() {
    clearWrapperAssets(false);
    clearTypedAssets(false);
  }

  @Override
  public void destroy() {
    if (manager != null) {
      manager.dispose();
      manager = null;
    }
    syntheticWrapperId = 0;
    for (FlixelWrapperFactory<?> f : wrapperFactories.values()) {
      f.clearAll();
    }
    wrapperFactories.clear();
    typedAssetCache.clear();
    audioPathCache.clear();
    extensionRegistry.clear();
  }

  @Override
  public void dispose() {
    destroy();
  }

  @NotNull
  @Override
  public String allocateSyntheticWrapperKey() {
    return "__flixel_syn_wrapper__/" + (syntheticWrapperId++);
  }

  @Override
  public void registerWrapper(@NotNull FlixelPooledWrapper wrapper) {
    FlixelWrapperFactory<?> factory = wrapperFactories.get(wrapper.wrapperRegistrationClass());
    if (factory == null) {
      throw new IllegalArgumentException(
        "No wrapper factory registered for: " + wrapper.wrapperRegistrationClass().getName()
          + ". Call registerWrapperFactory(...) first."
      );
    }
    registerWrapperUnchecked(factory, wrapper);
  }

  @Override
  public void registerWrapperFactory(@NotNull FlixelWrapperFactory<?> factory) {
    Class<?> type = factory.wrapperType();
    if (wrapperFactories.containsKey(type)) {
      throw new IllegalStateException("Wrapper factory already registered for: " + type.getName());
    }
    wrapperFactories.put(type, factory);
  }

  @SuppressWarnings("unchecked")
  private <W> void registerWrapperUnchecked(@NotNull FlixelWrapperFactory<W> factory, @NotNull FlixelPooledWrapper wrapper) {
    factory.registerInstance(this, (W) wrapper);
  }

  @NotNull
  @Override
  @SuppressWarnings("unchecked")
  public <W> W ensureWrapper(@NotNull String key, @NotNull Class<W> wrapperType) {
    FlixelWrapperFactory<?> f = wrapperFactories.get(wrapperType);
    if (f == null) {
      throw new IllegalArgumentException("No wrapper factory registered for: " + wrapperType.getName());
    }
    return ((FlixelWrapperFactory<W>) f).obtainKeyed(this, key);
  }

  @NotNull
  @Override
  public <W> W obtainWrapper(@NotNull String key, @NotNull Class<W> wrapperType) {
    W w = ensureWrapper(key, wrapperType);
    if (w instanceof FlixelAsset<?> fa) {
      fa.retain();
    }
    return w;
  }

  @Nullable
  @Override
  @SuppressWarnings("unchecked")
  public <W> W peekWrapper(@NotNull String key, @NotNull Class<W> wrapperType) {
    FlixelWrapperFactory<?> f = wrapperFactories.get(wrapperType);
    if (f == null) {
      throw new IllegalArgumentException("No wrapper factory registered for: " + wrapperType.getName());
    }
    return ((FlixelWrapperFactory<W>) f).peek(this, key);
  }

  @NotNull
  @Override
  @SuppressWarnings("unchecked")
  public <T> FlixelAsset<T> ensureTypedAsset(@NotNull String assetKey, @NotNull Class<T> type) {
    AssetId id = new AssetId(assetKey, type);
    FlixelTypedAsset<?> existing = typedAssetCache.get(id);
    if (existing != null) {
      return (FlixelAsset<T>) existing;
    }
    FlixelTypedAsset<T> created = new FlixelTypedAsset<>(this, assetKey, type);
    typedAssetCache.put(id, created);
    return created;
  }

  @NotNull
  @Override
  public <T> FlixelAsset<T> obtainTypedAsset(@NotNull String assetKey, @NotNull Class<T> type) {
    FlixelAsset<T> a = ensureTypedAsset(assetKey, type);
    a.retain();
    return a;
  }

  @Nullable
  @Override
  public FlixelAsset<?> peekTypedAsset(@NotNull String assetKey, @NotNull Class<?> type) {
    return typedAssetCache.get(new AssetId(assetKey, type));
  }

  @Override
  public void clearWrapperAssets(boolean respectPersist) {
    if (respectPersist) {
      for (FlixelWrapperFactory<?> f : wrapperFactories.values()) {
        f.clearNonPersist(this);
      }
    } else {
      for (FlixelWrapperFactory<?> f : wrapperFactories.values()) {
        f.clearAll();
      }
    }
  }

  @Override
  public void clearTypedAssets(boolean respectPersist) {
    Array<AssetId> toRemove = null;
    for (ObjectMap.Entry<AssetId, FlixelTypedAsset<?>> e : typedAssetCache) {
      FlixelAsset<?> a = e.value;
      if (a == null) continue;
      if (respectPersist && a.isPersist()) continue;
      if (a.getRefCount() > 0) continue;

      if (manager.isLoaded(a.getAssetKey(), a.getType())) {
        manager.unload(a.getAssetKey());
      }

      if (toRemove == null) {
        toRemove = new Array<>();
      }
      toRemove.add(e.key);
    }

    if (toRemove != null) {
      for (int i = 0; i < toRemove.size; i++) {
        typedAssetCache.remove(toRemove.get(i));
      }
    }
  }
}

