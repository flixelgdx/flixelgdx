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

import org.flixelgdx.Flixel;
import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.collections.FlixelMap;
import org.flixelgdx.file.FlixelFile;
import org.flixelgdx.functional.FlixelDestroyable;
import org.flixelgdx.graphics.FlixelGraphic;
import org.flixelgdx.graphics.FlixelImage;
import org.flixelgdx.graphics.FlixelTexture;
import org.flixelgdx.util.FlixelString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * The shared asset-manager pipeline every platform builds on.
 *
 * <p>This class implements the whole {@link FlixelAssetManager} contract with a synchronous,
 * single-threaded pipeline: loaders, the raw-content cache, wrapper handles, reference counting,
 * persist rules, and compressed-texture path resolution. Platforms with threads (see
 * {@code FlixelJvmAssetManager} in the JVM module) subclass it and override
 * {@link #submitLoad(PendingLoad)} to run stage one of each load on a worker; everything else is
 * inherited unchanged. That keeps the pipeline identical everywhere while letting each platform
 * choose how work is scheduled.
 *
 * <p>Reads flow through {@link #resolveFile(String)}, so a single installed
 * {@link FlixelAssetFileResolver} decides where assets come from on every code path.
 *
 * <p>Default loaders registered by the constructor:
 * <ul>
 *   <li>Images ({@code .png}, {@code .jpg}, {@code .jpeg}, {@code .bmp}, {@code .tga}) decode on
 *     the worker via {@link org.flixelgdx.graphics.FlixelGraphicsManager#decodeImage decodeImage}
 *     and upload on the main thread, producing {@link FlixelGraphic} handles.</li>
 *   <li>Text ({@code .txt}, {@code .xml}, {@code .json}) loads as {@link String} inside
 *     {@link FlixelDefaultAsset} handles.</li>
 * </ul>
 * Audio extensions are registered by the sound system once a sound factory is installed.
 */
public class FlixelBaseAssetManager implements FlixelAssetManager {

  private static final String[] TEXTURE_EXTENSIONS = { ".png", ".jpg", ".jpeg", ".bmp", ".tga" };

  @NotNull
  private final FlixelMap<String, FlixelAsset<?>> handles = new FlixelMap<>();

  @NotNull
  private final FlixelMap<String, Object> rawCache = new FlixelMap<>();

  @NotNull
  private final FlixelMap<String, FlixelAssetLoader<?>> loaders = new FlixelMap<>();

  @NotNull
  private final FlixelMap<String, String> texturePathCache = new FlixelMap<>();

  @NotNull
  private final FlixelArray<PendingLoad> pending = new FlixelArray<>();

  @Nullable
  private FlixelAssetFileResolver fileResolver;

  @NotNull
  private FlixelAssetMode assetMode = FlixelAssetMode.STANDARD;

  private int syntheticCounter;
  private int queuedThisBatch;
  private int finishedThisBatch;

  private boolean globalPersist;
  private boolean compressedTextures;

  /**
   * Creates a manager with the default loaders registered.
   */
  public FlixelBaseAssetManager() {
    FlixelAssetLoader<FlixelGraphic> imageLoader = new ImageLoader();
    for (String ext : TEXTURE_EXTENSIONS) {
      registerLoader(ext, imageLoader);
    }
    FlixelAssetLoader<String> textLoader = new TextLoader();
    registerLoader(".txt", textLoader);
    registerLoader(".xml", textLoader);
    registerLoader(".json", textLoader);
  }

  @Override
  public void load(@NotNull String path) {
    load(path, globalPersist);
  }

  @Override
  public void load(@NotNull String path, boolean persist) {
    String key = normalizeForLoad(path);
    if (rawCache.containsKey(key) || isQueued(key)) {
      FlixelAsset<?> existing = handles.get(key);
      if (existing != null && persist) {
        existing.setPersist(true);
      }
      return;
    }
    PendingLoad task = new PendingLoad(this, key, loaderFor(key), resolveFile(key), persist);
    pending.add(task);
    queuedThisBatch++;
    submitLoad(task);
  }

  @NotNull
  @Override
  @SuppressWarnings("unchecked")
  public <T> FlixelAsset<T> get(@NotNull String path) {
    String key = FlixelAssetPaths.normalizeAssetPath(path);
    FlixelAsset<?> handle = handles.get(key);
    if (handle == null) {
      handle = loaderFor(key).createHandle(this, key);
      handle.setPersist(globalPersist);
      handles.put(key, handle);
    }
    return (FlixelAsset<T>) handle;
  }

  @Nullable
  @Override
  public FlixelAsset<?> peek(@NotNull String path) {
    return handles.get(FlixelAssetPaths.normalizeAssetPath(path));
  }

  @Override
  public <T> void registerLoader(@NotNull String extension, @NotNull FlixelAssetLoader<T> loader) {
    loaders.put(normalizeExtension(extension), loader);
  }

  @Override
  public void unregisterLoader(@NotNull String extension) {
    loaders.remove(normalizeExtension(extension));
  }

  @Override
  public void register(@NotNull FlixelAsset<?> asset) {
    handles.put(asset.getPath(), asset);
  }

  @NotNull
  @Override
  public String allocateSyntheticKey() {
    return "flixel://synthetic/" + (syntheticCounter++);
  }

  @Override
  public boolean update() {
    return update(Integer.MAX_VALUE);
  }

  @Override
  public boolean update(int millis) {
    long deadline = System.currentTimeMillis() + millis;
    for (int i = pending.getSize() - 1; i >= 0; i--) {
      PendingLoad task = pending.get(i);
      if (!task.done) {
        continue;
      }
      pending.removeIndex(i);
      finishTask(task);
      if (System.currentTimeMillis() >= deadline) {
        break;
      }
    }
    boolean finished = pending.isEmpty();
    if (finished) {
      queuedThisBatch = 0;
      finishedThisBatch = 0;
    }
    return finished;
  }

  @Override
  public float getProgress() {
    if (queuedThisBatch == 0) {
      return 1f;
    }
    return Math.min(1f, finishedThisBatch / (float) queuedThisBatch);
  }

  @Override
  public int getLoadedAssetCount() {
    return handles.getSize();
  }

  @Override
  public boolean isLoaded(@NotNull String path) {
    String key = normalizeForLoad(path);
    return rawCache.containsKey(key);
  }

  @Override
  public void finishLoading() {
    while (!update()) {
      waitForLoads();
    }
  }

  @Override
  public void finishLoadingAsset(@NotNull String path) {
    String key = normalizeForLoad(path);
    while (isQueued(key)) {
      for (int i = 0; i < pending.getSize(); i++) {
        PendingLoad task = pending.get(i);
        if (task.path.equals(key) && task.done) {
          pending.removeIndex(i);
          finishTask(task);
          return;
        }
      }
      waitForLoads();
    }
  }

  @Override
  public void unload(@NotNull String path) {
    Object raw = rawCache.remove(FlixelAssetPaths.normalizeAssetPath(path));
    destroyRaw(raw);
  }

  @NotNull
  @Override
  public String getDiagnostics() {
    FlixelString out = new FlixelString(256);
    out.concat("Assets tracked: ").concat(handles.getSize()).concat('\n');
    for (FlixelMap.Entry<String, FlixelAsset<?>> entry : handles.entries()) {
      FlixelAsset<?> handle = entry.value;
      out.concat("  ").concat(entry.key)
          .concat(" refs=").concat(handle.getRefCount())
          .concat(" persist=").concat(handle.isPersist())
          .concat(" loaded=").concat(handle.isLoaded())
          .concat('\n');
    }
    return out.toString();
  }

  @Override
  public void clearNonPersist() {
    FlixelMap.Entries<String, FlixelAsset<?>> it = handles.entries();
    while (it.hasNext()) {
      FlixelAsset<?> handle = it.next().value;
      if (handle.getRefCount() <= 0 && !handle.isPersist()) {
        evict(handle);
        it.remove();
      }
    }
  }

  @Override
  public void clear() {
    for (FlixelMap.Entry<String, FlixelAsset<?>> entry : handles.entries()) {
      evict(entry.value);
    }
    handles.clear();
    for (FlixelMap.Entry<String, Object> entry : rawCache.entries()) {
      destroyRaw(entry.value);
    }
    rawCache.clear();
    pending.clear();
    queuedThisBatch = 0;
    finishedThisBatch = 0;
  }

  @Override
  public void destroy() {
    clear();
    texturePathCache.clear();
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
    this.assetMode = mode;
  }

  @Override
  public void onAssetReleased(@NotNull FlixelAsset<?> handle) {
    if (assetMode != FlixelAssetMode.AGGRESSIVE || handle.isPersist()) {
      return;
    }
    evict(handle);
    handles.remove(handle.getPath());
  }

  @Nullable
  @Override
  public Object getRaw(@NotNull String path) {
    return rawCache.get(FlixelAssetPaths.normalizeAssetPath(path));
  }

  @NotNull
  @Override
  public Object loadRawSync(@NotNull String path) {
    String key = FlixelAssetPaths.normalizeAssetPath(path);
    Object cached = rawCache.get(key);
    if (cached != null) {
      return cached;
    }
    FlixelAssetLoader<?> loader = loaderFor(key);
    try {
      Object raw = loader.loadRaw(this, key, resolveFile(key));
      Object finished = loader.finishRaw(this, key, raw);
      rawCache.put(key, finished);
      return finished;
    } catch (Exception e) {
      throw new IllegalStateException("Failed to load asset '" + key + "'.", e);
    }
  }

  @NotNull
  @Override
  public FlixelFile resolveFile(@NotNull String path) {
    FlixelAssetFileResolver resolver = fileResolver;
    if (resolver != null) {
      return resolver.resolve(path);
    }
    return Flixel.files.internal(path);
  }

  @Override
  public void setFileResolver(@Nullable FlixelAssetFileResolver resolver) {
    fileResolver = resolver;
  }

  @NotNull
  @Override
  public String resolveTexturePath(@NotNull String path) {
    String key = FlixelAssetPaths.normalizeAssetPath(path);
    if (!compressedTextures) {
      return key;
    }
    String cached = texturePathCache.get(key);
    if (cached != null) {
      return cached;
    }
    String resolved = key;
    int dot = key.lastIndexOf('.');
    if (dot > 0 && isTextureExtension(key.substring(dot))) {
      String sibling = key.substring(0, dot) + ".ktx2";
      if (loaders.containsKey(".ktx2") && resolveFile(sibling).exists()) {
        resolved = sibling;
      }
    }
    texturePathCache.put(key, resolved);
    return resolved;
  }

  @Override
  public void setCompressedTexturesEnabled(boolean enabled) {
    if (compressedTextures != enabled) {
      texturePathCache.clear();
    }
    compressedTextures = enabled;
  }

  @Override
  public boolean isCompressedTexturesEnabled() {
    return compressedTextures;
  }

  /**
   * Runs stage one of a queued load.
   *
   * <p>The base implementation runs it inline (single-threaded platforms). Subclasses with
   * threads override this to hand {@link PendingLoad#run()} to a worker.
   *
   * @param task The queued load to execute.
   */
  protected void submitLoad(@NotNull PendingLoad task) {
    task.run();
  }

  /**
   * Waits briefly for background loads to progress while blocking in
   * {@link #finishLoading()} or {@link #finishLoadingAsset(String)}.
   *
   * <p>The base implementation does nothing, because loads run inline. Threaded subclasses
   * override this to yield or sleep.
   */
  protected void waitForLoads() {}

  /** Applies stage two of a completed load and stores its result in the raw cache. */
  private void finishTask(@NotNull PendingLoad task) {
    finishedThisBatch++;
    if (task.error != null) {
      Flixel.error("Assets", "Failed to load asset '" + task.path + "'.", task.error);
      return;
    }
    Object raw = task.raw;
    if (raw == null) {
      return;
    }
    try {
      Object finished = task.loader.finishRaw(this, task.path, raw);
      rawCache.put(task.path, finished);
    } catch (Exception e) {
      Flixel.error("Assets", "Failed to finish asset '" + task.path + "'.", e);
      return;
    }
    FlixelAsset<?> handle = handles.get(task.path);
    if (handle == null) {
      handle = task.loader.createHandle(this, task.path);
      handles.put(task.path, handle);
    }
    handle.setPersist(task.persist);
  }

  /** Frees whatever the handle owns and its cached raw content. */
  private void evict(@NotNull FlixelAsset<?> handle) {
    if (handle instanceof FlixelGraphic graphic) {
      if (graphic.isOwned()) {
        FlixelTexture owned = graphic.getOwnedTexture();
        if (owned != null) {
          owned.destroy();
        }
        return;
      }
      destroyRaw(rawCache.remove(graphic.getResolvedPath()));
      return;
    }
    destroyRaw(rawCache.remove(handle.getPath()));
  }

  /** Destroys GPU or native resources held by raw content. */
  private void destroyRaw(@Nullable Object raw) {
    if (raw instanceof FlixelDestroyable destroyable) {
      destroyable.destroy();
    }
  }

  /** Normalizes a path and reroutes texture requests to their compressed sibling. */
  @NotNull
  private String normalizeForLoad(@NotNull String path) {
    return resolveTexturePath(FlixelAssetPaths.normalizeAssetPath(path));
  }

  private boolean isQueued(@NotNull String key) {
    for (int i = 0; i < pending.getSize(); i++) {
      if (pending.get(i).path.equals(key)) {
        return true;
      }
    }
    return false;
  }

  @NotNull
  private FlixelAssetLoader<?> loaderFor(@NotNull String path) {
    int dot = path.lastIndexOf('.');
    if (dot < 0) {
      throw new IllegalArgumentException("Asset path '" + path + "' has no file extension.");
    }
    FlixelAssetLoader<?> loader = loaders.get(path.substring(dot).toLowerCase());
    if (loader == null) {
      throw new IllegalArgumentException("No asset loader registered for '" + path + "'.");
    }
    return loader;
  }

  private static boolean isTextureExtension(@NotNull String extension) {
    String lower = extension.toLowerCase();
    for (String ext : TEXTURE_EXTENSIONS) {
      if (ext.equals(lower)) {
        return true;
      }
    }
    return false;
  }

  @NotNull
  private static String normalizeExtension(@NotNull String extension) {
    String ext = extension.toLowerCase();
    return ext.startsWith(".") ? ext : "." + ext;
  }

  /**
   * One queued load: the path, its loader, the resolved file, and the stage-one result.
   *
   * <p>{@link #run()} executes stage one and may run on a worker thread; the result fields are
   * volatile so the main thread sees them once {@link #done} flips.
   */
  protected static final class PendingLoad {

    @NotNull
    final FlixelAssetManager owner;

    @NotNull
    final String path;

    @NotNull
    final FlixelAssetLoader<?> loader;

    @NotNull
    final FlixelFile file;

    @Nullable
    volatile Object raw;

    @Nullable
    volatile Throwable error;

    final boolean persist;

    volatile boolean done;

    PendingLoad(@NotNull FlixelAssetManager owner, @NotNull String path, @NotNull FlixelAssetLoader<?> loader,
        @NotNull FlixelFile file, boolean persist) {
      this.owner = owner;
      this.path = path;
      this.loader = loader;
      this.file = file;
      this.persist = persist;
    }

    /** Executes stage one of the load, capturing the result or the failure. */
    public void run() {
      try {
        raw = loader.loadRaw(owner, path, file);
      } catch (Throwable t) {
        error = t;
      }
      done = true;
    }
  }

  /** Decodes image files into GPU textures wrapped in {@link FlixelGraphic} handles. */
  private static final class ImageLoader implements FlixelAssetLoader<FlixelGraphic> {

    @NotNull
    @Override
    public Object loadRaw(@NotNull FlixelAssetManager assets, @NotNull String path, @NotNull FlixelFile file)
        throws Exception {
      byte[] bytes = file.readBytes();
      if (bytes.length == 0) {
        throw new IllegalStateException("Image file not found or empty: '" + path + "'.");
      }
      ByteBuffer encoded = ByteBuffer.allocateDirect(bytes.length).order(ByteOrder.nativeOrder());
      encoded.put(bytes).flip();
      FlixelImage image = Flixel.graphics.decodeImage(encoded);
      if (image == null) {
        throw new IllegalStateException("Could not decode image: '" + path + "'.");
      }
      return image;
    }

    @NotNull
    @Override
    public Object finishRaw(@NotNull FlixelAssetManager assets, @NotNull String path, @NotNull Object raw) {
      if (raw instanceof FlixelImage image) {
        return Flixel.graphics.createTexture(image);
      }
      return raw;
    }

    @NotNull
    @Override
    public FlixelAsset<FlixelGraphic> createHandle(@NotNull FlixelAssetManager assets, @NotNull String path) {
      return new FlixelGraphic(assets, path);
    }
  }

  /** Loads text files as plain strings wrapped in {@link FlixelDefaultAsset} handles. */
  private static final class TextLoader implements FlixelAssetLoader<String> {

    @NotNull
    @Override
    public Object loadRaw(@NotNull FlixelAssetManager assets, @NotNull String path, @NotNull FlixelFile file) {
      return file.readString();
    }

    @NotNull
    @Override
    public FlixelAsset<String> createHandle(@NotNull FlixelAssetManager assets, @NotNull String path) {
      return new FlixelDefaultAsset<>(assets, path);
    }
  }
}
