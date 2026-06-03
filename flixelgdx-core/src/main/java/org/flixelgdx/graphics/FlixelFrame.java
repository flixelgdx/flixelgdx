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
package org.flixelgdx.graphics;

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
