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

import java.util.Objects;

/**
 * Frame wrapper around a libGDX {@link TextureRegion}.
 *
 * <p>This carries the extra metadata needed for Sparrow/atlas frames (original size and offsets),
 * similar to libGDX's {@code TextureAtlas.AtlasRegion}, but without depending on an atlas type.
 *
 * <h2>Why these extra fields exist</h2>
 * Sparrow (Starling) atlases ship <em>trimmed</em> frames: transparent borders are cut away to pack
 * more art into the texture. To draw a trimmed frame where the artist intended, the frame remembers
 * how big the art was <strong>before</strong> trimming ({@link #originalWidth}/{@link #originalHeight},
 * called the "source size") and where the trimmed pixels sit inside that original box
 * ({@link #offsetX}/{@link #offsetY}). Anchoring every frame to its source box is what keeps an
 * animated character's feet planted while individual frames change shape.
 *
 * <p>The static {@link #regionInsetX(int, int, int, boolean)} and
 * {@link #regionInsetY(int, int, int, boolean)} helpers turn that metadata into the pixel offset at
 * which the trimmed region should be drawn inside the source box. They are pure functions (no libGDX
 * state) so the geometry can be unit tested without a GPU texture.
 */
public final class FlixelFrame {

  @NotNull
  private final TextureRegion region;

  /** Optional frame name (used by Sparrow prefix animations). */
  @Nullable
  public String name;

  /**
   * Width of the original, untrimmed frame in pixels (Sparrow's {@code frameWidth}). For frames that
   * were never trimmed this equals {@link #getRegionWidth()}.
   */
  public int originalWidth;

  /** Height of the original, untrimmed frame in pixels (Sparrow's {@code frameHeight}). */
  public int originalHeight;

  /**
   * Horizontal offset (in pixels) from the source frame's left edge to the trimmed region's left
   * edge. This is the negation of Sparrow's {@code frameX} ({@code offsetX = -frameX}), so a frame
   * trimmed 13 pixels from the left ({@code frameX = -13}) stores {@code offsetX = 13}. Always
   * measured from the top-left of the source box, matching the image's natural orientation.
   */
  public int offsetX;

  /**
   * Vertical offset (in pixels) from the source frame's top edge to the trimmed region's top edge,
   * the negation of Sparrow's {@code frameY} ({@code offsetY = -frameY}).
   */
  public int offsetY;

  /**
   * Whether this frame was packed into its atlas rotated 90 degrees clockwise.
   *
   * <p>When {@code true}, Adobe Animate stores the sprite sideways in the PNG to save space.
   * The backing {@link TextureRegion} covers the on-disk footprint as-is (width = logical height,
   * height = logical width), while {@link #originalWidth} and {@link #originalHeight} always hold
   * the logical (pre-rotation) dimensions. The rig baker applies a rotation-correction matrix so
   * the part renders upright in rig space regardless of how it was packed.
   */
  public boolean rotated;

  /**
   * Constructs a new FlixelFrame with the given region.
   *
   * @param region The region to wrap.
   * @throws NullPointerException If the provided region is {@code null}.
   */
  public FlixelFrame(@NotNull TextureRegion region) {
    this.region = Objects.requireNonNull(region, "TextureRegion cannot be null.");
    this.name = null;
    this.originalWidth = region.getRegionWidth();
    this.originalHeight = region.getRegionHeight();
    this.offsetX = 0;
    this.offsetY = 0;
  }

  /**
   * Computes where the trimmed region's left edge sits inside the source box, in pixels measured
   * from the box's left edge.
   *
   * <p>When the frame is not horizontally flipped this is simply {@link #offsetX}. When it is
   * flipped, the art mirrors around the source box's vertical center line, so the left and right
   * trim gaps swap: the new left inset becomes whatever empty space used to sit on the right
   * ({@code sourceWidth - regionWidth - offsetX}). Mirroring around the source box (rather than the
   * trimmed region) is what keeps a left-facing pose lined up with its right-facing counterpart.
   *
   * @param sourceWidth The untrimmed frame width ({@link #originalWidth}).
   * @param regionWidth The trimmed region width ({@link #getRegionWidth()}).
   * @param offsetX The left trim offset ({@link #offsetX}).
   * @param flipX Whether the frame is drawn mirrored horizontally.
   * @return The left inset of the region inside the source box, in pixels.
   */
  public static int regionInsetX(int sourceWidth, int regionWidth, int offsetX, boolean flipX) {
    return flipX ? (sourceWidth - regionWidth - offsetX) : offsetX;
  }

  /**
   * Computes where the trimmed region's bottom edge sits inside the source box, in pixels measured
   * <strong>upward</strong> from the box's bottom edge.
   *
   * <p>The renderer works in a y-up space (larger y is higher on screen), so a region is positioned
   * by its bottom-left corner. The unflipped bottom inset is the empty space below the art inside
   * the source box ({@code sourceHeight - regionHeight - offsetY}); keeping it constant across an
   * animation's frames is exactly what plants a character's feet. A vertical flip swaps the top and
   * bottom gaps, leaving {@link #offsetY} as the new bottom inset.
   *
   * @param sourceHeight The untrimmed frame height ({@link #originalHeight}).
   * @param regionHeight The trimmed region height ({@link #getRegionHeight()}).
   * @param offsetY The top trim offset ({@link #offsetY}).
   * @param flipY Whether the frame is drawn mirrored vertically.
   * @return The bottom inset of the region inside the source box, in pixels.
   */
  public static int regionInsetY(int sourceHeight, int regionHeight, int offsetY, boolean flipY) {
    return flipY ? offsetY : (sourceHeight - regionHeight - offsetY);
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
