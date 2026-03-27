/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.asset;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import games.rednblack.miniaudio.MASound;
import games.rednblack.miniaudio.loader.MASoundLoader;
import me.stringdotjar.flixelgdx.Flixel;
import me.stringdotjar.flixelgdx.audio.FlixelSoundSource;
import me.stringdotjar.flixelgdx.audio.FlixelSoundSourceLoader;
import me.stringdotjar.flixelgdx.graphics.FlixelGraphic;
import me.stringdotjar.flixelgdx.graphics.FlixelGraphicSource;
import me.stringdotjar.flixelgdx.graphics.FlixelGraphicSourceLoader;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Default concrete asset manager for FlixelGDX.
 *
 * <p>This class centralizes asset loading and lifecycle policy in one place. It wraps a single libGDX
 * {@link AssetManager}, plus pooled wrappers ({@link FlixelGraphic}, {@link FlixelAsset})
 * that track {@code persist} and reference counts and can be cleared on state switches.
 *
 * <p><b>Recommended usage:</b> Access this via {@link me.stringdotjar.flixelgdx.Flixel#assets}.
 *
 * <p><b>Experts:</b> {@link #getManager()} exposes the underlying {@link AssetManager} for custom
 * loaders, {@link AssetDescriptor} batches, or APIs not wrapped here.
 */
public class FlixelDefaultAssetManager implements FlixelAssetManager {

  private AssetManager manager;

  private final ConcurrentHashMap<String, String> audioPathCache = new ConcurrentHashMap<>();

  private final ObjectMap<String, FlixelGraphic> graphicCache = new ObjectMap<>();
  private int ownedGraphicId;

  private final ObjectMap<AssetId, FlixelAsset<?>> typedAssetCache = new ObjectMap<>();

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

  /** Constructs a new asset manager with the default loaders for audio, strings, and sound sources. */
  public FlixelDefaultAssetManager() {
    manager = new AssetManager();
    ensureMiniAudioLoader();
    manager.setLoader(String.class, new FlixelStringAssetLoader(manager.getFileHandleResolver()));
    manager.setLoader(FlixelGraphicSource.class, new FlixelGraphicSourceLoader(manager.getFileHandleResolver()));
    manager.setLoader(FlixelSoundSource.class, new FlixelSoundSourceLoader(manager.getFileHandleResolver()));
  }

  /**
   * Registers (or re-registers) the MiniAudio {@link MASound} loader, if the global audio system is available.
   *
   * <p>This is a no-op until {@link me.stringdotjar.flixelgdx.Flixel#sound} is initialized.
   */
  public void ensureMiniAudioLoader() {
    if (Flixel.sound == null) {
      return;
    }
    manager.setLoader(MASound.class, new MASoundLoader(Flixel.sound.getEngine(), manager.getFileHandleResolver()));
  }

  /** Returns the underlying libGDX {@link AssetManager}. */
  @NotNull
  @Override
  public AssetManager getManager() {
    return manager;
  }

  @Override
  public <T> void load(@NotNull String fileName, @NotNull Class<T> type) {
    manager.load(fileName, type);
  }

  @Override
  public void load(@NotNull FlixelSource<?> source) {
    if (source == null) {
      throw new IllegalArgumentException("Source cannot be null.");
    }
    load(source.getAssetKey(), source.getType());
  }

  @Override
  public void load(@NotNull AssetDescriptor<?> assetDescriptor) {
    manager.load(assetDescriptor);
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
  public Texture requireTexture(@NotNull String assetKey) {
    if (!manager.isLoaded(assetKey, Texture.class)) {
      throw new IllegalStateException(
        "Texture not loaded: \"" + assetKey + "\". Preload it in a loading state (Flixel.assets.load + Flixel.assets.update()), "
          + "or call loadGraphicSourceNow(String) explicitly."
      );
    }
    return manager.get(assetKey, Texture.class);
  }

  @NotNull
  @Override
  public Texture loadGraphicSourceNow(@NotNull String assetKey) {
    if (!manager.isLoaded(assetKey, FlixelGraphicSource.class)) {
      manager.load(assetKey, FlixelGraphicSource.class);
      manager.finishLoadingAsset(assetKey);
    }
    return requireTexture(assetKey);
  }

  @NotNull
  @Override
  public String resolveAudioPath(@NotNull String path) {
    return audioPathCache.computeIfAbsent(path, this::extractAssetPath);
  }

  @NotNull
  @Override
  public String extractAssetPath(@NotNull String path) {
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
    return manager.getDiagnostics();
  }

  @Override
  public void clearNonPersist() {
    clearNonPersistGraphics();
    clearNonPersistTypedAssets();
  }

  @Override
  public void destroy() {
    if (manager != null) {
      manager.dispose();
      manager = null;
    }
    ownedGraphicId = 0;
    graphicCache.clear();
    typedAssetCache.clear();
    audioPathCache.clear();
  }

  @Override
  public void dispose() {
    destroy();
  }

  @NotNull
  @Override
  public FlixelGraphic obtainGraphic(@NotNull String assetKey) {
    FlixelGraphic g = graphicCache.get(assetKey);
    if (g == null) {
      g = new FlixelGraphic(this, assetKey);
      graphicCache.put(assetKey, g);
    }
    return g;
  }

  @NotNull
  @Override
  public FlixelGraphic obtainOwnedGraphic(@NotNull Texture texture) {
    String key = "__owned_texture__/" + (ownedGraphicId++);
    FlixelGraphic g = new FlixelGraphic(this, key, texture);
    graphicCache.put(key, g);
    return g;
  }

  @Nullable
  @Override
  public FlixelGraphic peekGraphic(@NotNull String assetKey) {
    return graphicCache.get(assetKey);
  }

  @Override
  public void clearNonPersistGraphics() {
    AssetManager assets = manager;

    Array<String> toRemove = null;
    for (ObjectMap.Entry<String, FlixelGraphic> e : graphicCache) {
      FlixelGraphic g = e.value;
      if (g == null) continue;
      if (g.persist) continue;
      if (g.getRefCount() > 0) continue;

      if (g.isOwned()) {
        Texture t = g.getOwnedTexture();
        if (t != null) {
          t.dispose();
        }
      } else if (assets != null) {
        if (assets.isLoaded(g.getAssetKey())) {
          assets.unload(g.getAssetKey());
        }
      }

      if (toRemove == null) {
        toRemove = new Array<>();
      }
      toRemove.add(g.getAssetKey());
    }

    if (toRemove != null) {
      for (int i = 0; i < toRemove.size; i++) {
        graphicCache.remove(toRemove.get(i));
      }
    }
  }

  @NotNull
  @Override
  @SuppressWarnings("unchecked")
  public <T> FlixelAsset<T> obtainTypedAsset(@NotNull String assetKey, @NotNull Class<T> type) {
    AssetId id = new AssetId(assetKey, type);
    FlixelAsset<?> existing = typedAssetCache.get(id);
    if (existing != null) {
      return (FlixelAsset<T>) existing;
    }
    FlixelAsset<T> created = new FlixelAsset<>(this, assetKey, type);
    typedAssetCache.put(id, created);
    return created;
  }

  @Nullable
  @Override
  public FlixelAsset<?> peekTypedAsset(@NotNull String assetKey, @NotNull Class<?> type) {
    return typedAssetCache.get(new AssetId(assetKey, type));
  }

  @Override
  public void clearNonPersistTypedAssets() {

    Array<AssetId> toRemove = null;
    for (ObjectMap.Entry<AssetId, FlixelAsset<?>> e : typedAssetCache) {
      FlixelAsset<?> a = e.value;
      if (a == null) continue;
      if (a.isPersist()) continue;
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

