package me.stringdotjar.flixelgdx.graphics;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import me.stringdotjar.flixelgdx.Flixel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Graphic container and wrapper around a libGDX {@link Texture}.
 *
 * <p>Graphics are identified by an {@code assetKey} (usually an internal asset path).
 * Instances are cached globally so multiple sprites can share the same texture.
 *
 * <p>This wrapper implements lifecycle policy (refcount + {@code persist}).
 * Actual loading/caching is performed by libGDX's {@link AssetManager} ({@link Flixel#assets}).
 */
public final class FlixelGraphic {

  private static final ObjectMap<String, FlixelGraphic> CACHE = new ObjectMap<>();
  private static int ownedId = 0;

  @NotNull
  private final String assetKey;

  /** If true, this graphic will not be auto-unloaded on state switches. */
  public boolean persist;
  private int refCount;

  @Nullable
  private final Texture ownedTexture;

  private final boolean owned;

  private FlixelGraphic(@NotNull String assetKey) {
    if (assetKey == null || assetKey.isEmpty()) {
      throw new IllegalArgumentException("assetKey cannot be null/empty");
    }
    this.assetKey = assetKey;
    this.persist = false;
    this.refCount = 0;
    this.ownedTexture = null;
    this.owned = false;
  }

  private FlixelGraphic(@NotNull String syntheticKey, @NotNull Texture ownedTexture) {
    this.assetKey = syntheticKey;
    this.persist = false;
    this.refCount = 0;
    this.ownedTexture = ownedTexture;
    this.owned = true;
  }

  /** Returns the cache key (typically an internal path like {@code "images/player.png"}). */
  @NotNull
  public String getAssetKey() {
    return assetKey;
  }

  public FlixelGraphic setPersist(boolean persist) {
    this.persist = persist;
    return this;
  }

  public int getRefCount() {
    return refCount;
  }

  /** Increases the external reference count for this graphic. */
  public FlixelGraphic retain() {
    refCount++;
    return this;
  }

  /** Decreases the external reference count for this graphic (never below 0). */
  public FlixelGraphic release() {
    refCount--;
    if (refCount < 0) {
      refCount = 0;
    }
    return this;
  }

  /**
   * Enqueues this texture into {@link Flixel#assets}. Call from a loading state.
   * Safe to call multiple times.
   */
  public void queueLoad() {
    if (owned) {
      return;
    }
    AssetManager assets = Flixel.assets;
    if (assets == null) {
      throw new IllegalStateException("Flixel.assets is null (Flixel.initialize() not called yet).");
    }
    if (!assets.isLoaded(assetKey, Texture.class)) {
      assets.load(assetKey, Texture.class);
    }
  }

  /**
   * Returns the loaded texture. This is a strict API: the texture must already be loaded
   * (typically via {@link #queueLoad()} + {@code AssetManager.update()} in a loading state).
   */
  @NotNull
  public Texture requireTexture() {
    if (owned) {
      return ownedTexture;
    }
    AssetManager assets = Flixel.assets;
    if (assets == null) {
      throw new IllegalStateException("Flixel.assets is null (Flixel.initialize() not called yet).");
    }
    if (!assets.isLoaded(assetKey, Texture.class)) {
      throw new IllegalStateException(
        "Texture not loaded: \"" + assetKey + "\". Preload it in a loading state (assets.load() + assets.update()), "
          + "or call loadNow() explicitly."
      );
    }
    return assets.get(assetKey, Texture.class);
  }

  /**
   * Explicit synchronous load for one-off cases if the texture is not loaded yet.
   *
   * <p>This should not be used implicitly during gameplay. It is recommended to use
   * {@link #queueLoad()} beforehand instead.
   */
  @NotNull
  public Texture loadNow() {
    if (owned) {
      return ownedTexture;
    }
    AssetManager assets = Flixel.assets;
    if (assets == null) {
      throw new IllegalStateException("Flixel.assets is null (Flixel.initialize() not called yet).");
    }
    if (!assets.isLoaded(assetKey, Texture.class)) {
      assets.load(assetKey, Texture.class);
      assets.finishLoadingAsset(assetKey);
    }
    return assets.get(assetKey, Texture.class);
  }

  /**
   * Gets or creates a cached graphic wrapper for the given key.
   */
  @NotNull
  public static FlixelGraphic get(@NotNull String assetKey) {
    FlixelGraphic g = CACHE.get(assetKey);
    if (g == null) {
      g = new FlixelGraphic(assetKey);
      CACHE.put(assetKey, g);
    }
    return g;
  }

  /**
   * Wraps an externally created texture (e.g. from {@link com.badlogic.gdx.graphics.Pixmap})
   * as an owned graphic. Owned textures are disposed when cleared (or when unloaded due to being
   * non-persistent with zero refs).
   */
  @NotNull
  public static FlixelGraphic owned(@NotNull Texture texture) {
    if (texture == null) {
      throw new IllegalArgumentException("texture cannot be null");
    }
    String key = "__owned_texture__/" + (ownedId++);
    FlixelGraphic g = new FlixelGraphic(key, texture);
    CACHE.put(key, g);
    return g;
  }

  /**
   * Removes and unloads all cached graphics that are non-persistent and have no external refs.
   * Call from {@link me.stringdotjar.flixelgdx.Flixel#switchState} after the old state is destroyed.
   */
  public static void clearNonPersist() {
    AssetManager assets = Flixel.assets;
    // NOTE: assets may be null early; owned textures can still be cleared safely.

    Array<String> toRemove = null;
    for (ObjectMap.Entry<String, FlixelGraphic> e : CACHE) {
      FlixelGraphic g = e.value;
      if (g == null) continue;
      if (g.persist) continue;
      if (g.refCount > 0) continue;

      if (g.owned) {
        if (g.ownedTexture != null) {
          g.ownedTexture.dispose();
        }
      } else if (assets != null) {
        if (assets.isLoaded(g.assetKey, Texture.class)) {
          assets.unload(g.assetKey);
        }
      }

      if (toRemove == null) {
        toRemove = new Array<>();
      }
      toRemove.add(g.assetKey);
    }

    if (toRemove != null) {
      for (int i = 0; i < toRemove.size; i++) {
        CACHE.remove(toRemove.get(i));
      }
    }
  }

  /**
   * Returns the cached wrapper if present, otherwise null.
   */
  @Nullable
  public static FlixelGraphic peek(@NotNull String assetKey) {
    return CACHE.get(assetKey);
  }
}

