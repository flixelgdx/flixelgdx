/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.graphics;

import com.badlogic.gdx.graphics.Texture;

import me.stringdotjar.flixelgdx.asset.FlixelAssetManager;
import me.stringdotjar.flixelgdx.asset.FlixelSource;
import me.stringdotjar.flixelgdx.asset.FlixelTypedAsset;
import me.stringdotjar.flixelgdx.asset.FlixelPooledWrapper;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Graphic container and wrapper around a libGDX {@link Texture}, implementing {@link me.stringdotjar.flixelgdx.asset.FlixelAsset}{@code <Texture>}
 * via {@link me.stringdotjar.flixelgdx.asset.FlixelTypedAsset}.
 *
 * <p>Graphics are identified by an {@code assetKey} (usually an internal asset path).
 * Wrapper instances are pooled in {@link me.stringdotjar.flixelgdx.asset.FlixelAssetManager} so
 * multiple sprites can share policy state.
 *
 * <p>Lifecycle ({@code persist}, refcount) is tracked here; keyed texture loading is implemented in
 * {@link me.stringdotjar.flixelgdx.asset.FlixelAssetManager} ({@link me.stringdotjar.flixelgdx.Flixel#assets}).
 *
 * <p>Enqueue loads with {@link #queueLoad()} (or {@link me.stringdotjar.flixelgdx.asset.FlixelAssetManager#load(FlixelSource)} /
 * {@link me.stringdotjar.flixelgdx.asset.FlixelAssetManager#load(String)} on {@code Flixel.assets}); prefer
 * {@link me.stringdotjar.flixelgdx.asset.FlixelAssetManager#load(FlixelSource)} over {@code load(String)} when the type must be explicit.
 *
 * @see me.stringdotjar.flixelgdx.asset.FlixelAssetManager
 */
public final class FlixelGraphic extends FlixelTypedAsset<Texture> implements FlixelPooledWrapper {

  @Nullable
  private final Texture ownedTexture;

  private final boolean owned;

  public FlixelGraphic(@NotNull FlixelAssetManager assetManager, @NotNull String assetKey) {
    this(assetManager, assetKey, null);
  }

  public FlixelGraphic(@NotNull FlixelAssetManager assetManager, @NotNull String key, @Nullable Texture ownedTexture) {
    super(assetManager, key, Texture.class);
    this.owned = (ownedTexture != null);
    this.ownedTexture = ownedTexture;
  }

  @NotNull
  @Override
  public FlixelGraphic setPersist(boolean persist) {
    super.setPersist(persist);
    return this;
  }

  @NotNull
  @Override
  public FlixelGraphic retain() {
    super.retain();
    return this;
  }

  @NotNull
  @Override
  public FlixelGraphic release() {
    super.release();
    return this;
  }

  /** Whether this graphic wraps a pixmap-generated texture (disposed on clear, not unloaded by key). */
  @Override
  public boolean isOwned() {
    return owned;
  }

  /** The owned texture when {@link #isOwned()} is {@code true}, else {@code null}. */
  @Nullable
  public Texture getOwnedTexture() {
    return ownedTexture;
  }

  @Override
  public void queueLoad() {
    if (owned) {
      return;
    }
    super.queueLoad();
  }

  /**
   * Returns the loaded texture. Strict: the texture must already be loaded
   * (typically via {@link #queueLoad()} plus {@link FlixelAssetManager#update()} in a loading state).
   */
  @NotNull
  @Override
  public Texture require() {
    if (owned) {
      return Objects.requireNonNull(ownedTexture);
    }
    return super.require();
  }

  /** Same as {@link #require()}; kept for call sites that name the texture explicitly. */
  @NotNull
  public Texture requireTexture() {
    return require();
  }

  /**
   * Explicit synchronous load for one-off cases if the texture is not loaded yet.
   *
   * <p>Prefer {@link #queueLoad()} in a loading state to avoid blocking the main thread.
   */
  @NotNull
  @Override
  public Texture loadNow() {
    if (owned) {
      return Objects.requireNonNull(ownedTexture);
    }
    return super.loadNow();
  }
}
