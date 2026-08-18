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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * A rectangular region of a {@link FlixelTexture}: one frame of a sprite sheet or atlas.
 *
 * <p>This carries the extra metadata needed for Sparrow/atlas frames (original size and offsets),
 * so a single texture can hold many frames without per-frame textures.
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
 * which the trimmed region should be drawn inside the source box. They are pure functions (no GPU
 * state) so the geometry can be unit tested without a texture.
 */
public final class FlixelFrame {

  @NotNull
  private final FlixelTexture texture;

  private final int regionX;
  private final int regionY;
  private final int regionWidth;
  private final int regionHeight;

  private final float u;
  private final float v;
  private final float u2;
  private final float v2;

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
   * The region covers the on-disk footprint as-is (width = logical height, height = logical
   * width), while {@link #originalWidth} and {@link #originalHeight} always hold the logical
   * (pre-rotation) dimensions. The rig baker applies a rotation-correction matrix so the part
   * renders upright in rig space regardless of how it was packed.
   */
  public boolean rotated;

  /**
   * Constructs a frame that covers an entire texture.
   *
   * @param texture The texture to wrap.
   * @throws NullPointerException If the provided texture is {@code null}.
   */
  public FlixelFrame(@NotNull FlixelTexture texture) {
    this(texture, 0, 0, texture.getWidth(), texture.getHeight());
  }

  /**
   * Constructs a frame covering the given rectangle of a texture, in pixels measured from the
   * texture's top-left corner.
   *
   * @param texture The texture the region lives in.
   * @param regionX Left edge of the region in pixels.
   * @param regionY Top edge of the region in pixels.
   * @param regionWidth Region width in pixels.
   * @param regionHeight Region height in pixels.
   * @throws NullPointerException If the provided texture is {@code null}.
   */
  public FlixelFrame(@NotNull FlixelTexture texture, int regionX, int regionY, int regionWidth, int regionHeight) {
    this.texture = Objects.requireNonNull(texture, "Texture cannot be null.");
    this.regionX = regionX;
    this.regionY = regionY;
    this.regionWidth = regionWidth;
    this.regionHeight = regionHeight;
    float texW = Math.max(1, texture.getWidth());
    float texH = Math.max(1, texture.getHeight());
    this.u = regionX / texW;
    this.v = regionY / texH;
    this.u2 = (regionX + regionWidth) / texW;
    this.v2 = (regionY + regionHeight) / texH;
    this.name = null;
    this.originalWidth = regionWidth;
    this.originalHeight = regionHeight;
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
   * Computes where the trimmed region's top edge sits inside the source box, in pixels measured
   * <strong>downward</strong> from the box's top edge.
   *
   * <p>The renderer works in a y-down space (larger y is lower on screen), so a region is positioned
   * by its top-left corner. The unflipped top inset is simply the empty space above the art inside
   * the source box ({@link #offsetY}); keeping it constant across an animation's frames is exactly
   * what keeps a character steady. A vertical flip swaps the top and bottom gaps, leaving
   * {@code sourceHeight - regionHeight - offsetY} as the new top inset.
   *
   * @param sourceHeight The untrimmed frame height ({@link #originalHeight}).
   * @param regionHeight The trimmed region height ({@link #getRegionHeight()}).
   * @param offsetY The top trim offset ({@link #offsetY}).
   * @param flipY Whether the frame is drawn mirrored vertically.
   * @return The top inset of the region inside the source box, in pixels.
   */
  public static int regionInsetY(int sourceHeight, int regionHeight, int offsetY, boolean flipY) {
    return flipY ? (sourceHeight - regionHeight - offsetY) : offsetY;
  }

  @NotNull
  public FlixelTexture getTexture() {
    return texture;
  }

  public int getRegionX() {
    return regionX;
  }

  public int getRegionY() {
    return regionY;
  }

  public int getRegionWidth() {
    return regionWidth;
  }

  public int getRegionHeight() {
    return regionHeight;
  }

  /** Returns the left texture coordinate of this region in {@code [0, 1]}. */
  public float getU() {
    return u;
  }

  /** Returns the top texture coordinate of this region in {@code [0, 1]}. */
  public float getV() {
    return v;
  }

  /** Returns the right texture coordinate of this region in {@code [0, 1]}. */
  public float getU2() {
    return u2;
  }

  /** Returns the bottom texture coordinate of this region in {@code [0, 1]}. */
  public float getV2() {
    return v2;
  }
}
