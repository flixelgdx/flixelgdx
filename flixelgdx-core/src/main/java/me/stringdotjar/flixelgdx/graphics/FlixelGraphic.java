/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.graphics;

import com.badlogic.gdx.graphics.Texture;

import me.stringdotjar.flixelgdx.asset.FlixelAssetManager;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Graphic container and wrapper around a libGDX {@link Texture}.
 *
 * <p>Graphics are identified by an {@code assetKey} (usually an internal asset path).
 * Wrapper instances are pooled in {@link me.stringdotjar.flixelgdx.asset.FlixelAssetManager} so
 * multiple sprites can share policy state.
 *
 * <p>Lifecycle ({@code persist}, refcount) is tracked here; keyed texture loading is implemented in
 * {@link me.stringdotjar.flixelgdx.asset.FlixelAssetManager} ({@link me.stringdotjar.flixelgdx.Flixel#assets}).
 */
public final class FlixelGraphic {

  @NotNull
  private final String assetKey;

  /** If true, this graphic will not be auto-unloaded on state switches. */
  public boolean persist;
  private int refCount;

  @NotNull
  private final FlixelAssetManager assetManager;

  @Nullable
  private final Texture ownedTexture;

  private final boolean owned;

  public FlixelGraphic(@NotNull FlixelAssetManager assetManager, @NotNull String assetKey) {
    this(assetManager, assetKey, null);
  }

  public FlixelGraphic(@NotNull FlixelAssetManager assetManager, @NotNull String key, @Nullable Texture ownedTexture) {
    if (key == null || key.isEmpty()) {
      throw new IllegalArgumentException("Asset key cannot be null/empty.");
    }
    this.assetKey = key;
    this.persist = false;
    this.refCount = 0;
    this.assetManager = assetManager;
    this.owned = (ownedTexture != null);
    this.ownedTexture = ownedTexture;
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

  /** Whether this graphic wraps a pixmap-generated texture (unloaded via {@link FlixelAssets}). */
  public boolean isOwned() {
    return owned;
  }

  /** The owned texture when {@link #isOwned()} is {@code true}, else {@code null}. */
  @Nullable
  public Texture getOwnedTexture() {
    return ownedTexture;
  }

  /**
   * Enqueues this texture into {@link FlixelAssets}. Call from a loading state.
   * Safe to call multiple times.
   */
  public void queueLoad() {
    if (owned) {
      return;
    }
    assetManager.load(new FlixelGraphicSource(assetKey));
  }

  /**
   * Returns the loaded texture. Strict: the texture must already be loaded
   * (typically via {@link #queueLoad()} + {@link FlixelAssetManager#update()} / {@link Flixel.assets#update()} in a loading state).
   */
  @NotNull
  public Texture requireTexture() {
    if (owned) {
      return Objects.requireNonNull(ownedTexture);
    }
    return assetManager.requireTexture(assetKey);
  }

  /**
   * Explicit synchronous load for one-off cases if the texture is not loaded yet.
   *
   * <p>Prefer {@link #queueLoad()} in a loading state to avoid blocking the main thread.
   */
  @NotNull
  public Texture loadNow() {
    if (owned) {
      return Objects.requireNonNull(ownedTexture);
    }
    return assetManager.loadGraphicSourceNow(assetKey);
  }
}
