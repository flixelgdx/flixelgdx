package me.stringdotjar.flixelgdx.assets;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import me.stringdotjar.flixelgdx.Flixel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Generic asset wrapper built on libGDX {@link AssetManager}.
 *
 * <p>This provides lifecycle policy (refcount + {@code persist}) while delegating actual
 * loading/caching to {@link Flixel#assets}.
 *
 * <p>Loading policy is explicit: call {@link #queueLoad()} from a loading state and advance
 * loading via {@code Flixel.assets.update()}. Use {@link #require()} in gameplay states. Use
 * {@link #loadNow()} for one-off cases if the asset is not loaded yet.
 */
public final class FlixelAsset<T> {

  private static final ObjectMap<AssetId, FlixelAsset<?>> CACHE = new ObjectMap<>();

  private static final class AssetId {
    final String key;
    final Class<?> type;

    AssetId(String key, Class<?> type) {
      this.key = key;
      this.type = type;
    }

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

  @NotNull
  private final String assetKey;

  @NotNull
  private final Class<T> type;

  private boolean persist;
  private int refCount;

  private FlixelAsset(@NotNull String assetKey, @NotNull Class<T> type) {
    if (assetKey == null || assetKey.isEmpty()) {
      throw new IllegalArgumentException("assetKey cannot be null/empty");
    }
    if (type == null) {
      throw new IllegalArgumentException("type cannot be null");
    }
    this.assetKey = assetKey;
    this.type = type;
    this.persist = false;
    this.refCount = 0;
  }

  @NotNull
  public String getAssetKey() {
    return assetKey;
  }

  @NotNull
  public Class<T> getType() {
    return type;
  }

  public boolean isPersist() {
    return persist;
  }

  public FlixelAsset<T> setPersist(boolean persist) {
    this.persist = persist;
    return this;
  }

  public int getRefCount() {
    return refCount;
  }

  public FlixelAsset<T> retain() {
    refCount++;
    return this;
  }

  public FlixelAsset<T> release() {
    refCount--;
    if (refCount < 0) {
      refCount = 0;
    }
    return this;
  }

  /**
   * Enqueues this asset into {@link Flixel#assets}. Safe to call multiple times.
   *
   * <p>Use this method to preload assets in your game's loading state.
   */
  public void queueLoad() {
    AssetManager assets = Flixel.getAssets();
    if (assets == null) {
      throw new IllegalStateException("Flixel.assets is null (Flixel.initialize() not called yet).");
    }
    if (!assets.isLoaded(assetKey, type)) {
      assets.load(assetKey, type);
    }
  }

  /** Strict: requires the asset to already be loaded. */
  @NotNull
  public T require() {
    AssetManager assets = Flixel.getAssets();
    if (assets == null) {
      throw new IllegalStateException("Flixel.assets is null (Flixel.initialize() not called yet).");
    }
    if (!assets.isLoaded(assetKey, type)) {
      throw new IllegalStateException(
        "Asset not loaded: \"" + assetKey + "\" (" + type.getSimpleName() + "). "
          + "Preload it in a loading state (assets.load + assets.update), or call loadNow() explicitly."
      );
    }
    return assets.get(assetKey, type);
  }

  /**
   * Explicit synchronous load for one-off cases; avoid using implicitly during gameplay.
   *
   * <p>Use this method to load an asset that is not loaded yet. Note that this method will block
   * the game's update loop until the asset is loaded, so your game might freeze if you use this.
   */
  @NotNull
  public T loadNow() {
    AssetManager assets = Flixel.getAssets();
    if (assets == null) {
      throw new IllegalStateException("Flixel.assets is null (Flixel.initialize() not called yet).");
    }
    if (!assets.isLoaded(assetKey, type)) {
      assets.load(assetKey, type);
      assets.finishLoadingAsset(assetKey);
    }
    return assets.get(assetKey, type);
  }

  @SuppressWarnings("unchecked")
  @NotNull
  public static <T> FlixelAsset<T> get(@NotNull String assetKey, @NotNull Class<T> type) {
    AssetId id = new AssetId(assetKey, type);
    FlixelAsset<?> existing = CACHE.get(id);
    if (existing != null) {
      return (FlixelAsset<T>) existing;
    }
    FlixelAsset<T> created = new FlixelAsset<>(assetKey, type);
    CACHE.put(id, created);
    return created;
  }

  /** Unloads and removes all non-persistent, unreferenced assets from the wrapper cache. */
  public static void clearNonPersist() {
    AssetManager assets = Flixel.getAssets();
    if (assets == null) {
      return;
    }

    Array<AssetId> toRemove = null;
    for (ObjectMap.Entry<AssetId, FlixelAsset<?>> e : CACHE) {
      FlixelAsset<?> a = e.value;
      if (a == null) continue;
      if (a.persist) continue;
      if (a.refCount > 0) continue;

      if (assets.isLoaded(a.assetKey, a.type)) {
        assets.unload(a.assetKey);
      }

      if (toRemove == null) {
        toRemove = new Array<>();
      }
      toRemove.add(e.key);
    }

    if (toRemove != null) {
      for (int i = 0; i < toRemove.size; i++) {
        CACHE.remove(toRemove.get(i));
      }
    }
  }

  @Nullable
  public static FlixelAsset<?> peek(@NotNull String assetKey, @NotNull Class<?> type) {
    return CACHE.get(new AssetId(assetKey, type));
  }
}

