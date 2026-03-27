/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.graphics;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * HaxeFlixel-like frame wrapper around a libGDX {@link TextureRegion}.
 *
 * <p>This carries the extra metadata needed for Sparrow/atlas frames (original size and offsets),
 * similar to libGDX's {@code TextureAtlas.AtlasRegion}, but without depending on an atlas type.
 */
public final class FlixelFrame {

  @NotNull
  private final TextureRegion region;

  /** Optional frame name (used by Sparrow prefix animations). */
  @Nullable
  public String name;

  // Original (uncropped) frame width/height.
  public int originalWidth;
  public int originalHeight;

  // Offset from the top-left of the original frame to the region (pixels).
  public int offsetX;
  public int offsetY;

  /**
   * Constructs a new FlixelFrame with the given region.
   *
   * @param region The region to wrap.
   */
  public FlixelFrame(@NotNull TextureRegion region) {
    if (region == null) {
      throw new IllegalArgumentException("TextureRegion cannot be null.");
    }
    this.region = region;
    this.name = null;
    this.originalWidth = region.getRegionWidth();
    this.originalHeight = region.getRegionHeight();
    this.offsetX = 0;
    this.offsetY = 0;
  }

  @NotNull
  public TextureRegion getRegion() {
    return region;
  }

  @NotNull
  public Texture getTexture() {
    return region.getTexture();
  }

  public int getRegionX() {
    return region.getRegionX();
  }

  public int getRegionY() {
    return region.getRegionY();
  }

  public int getRegionWidth() {
    return region.getRegionWidth();
  }

  public int getRegionHeight() {
    return region.getRegionHeight();
  }
}

