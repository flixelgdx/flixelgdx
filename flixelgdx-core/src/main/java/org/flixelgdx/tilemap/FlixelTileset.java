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
package org.flixelgdx.tilemap;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import org.flixelgdx.graphics.FlixelGraphic;
import org.jetbrains.annotations.Nullable;

/**
 * Slices a tile sheet into a flat, ID-indexed array of {@link TextureRegion}s once, at
 * construction time, so drawing a tilemap never allocates.
 *
 * <p>The tile sheet is a plain grid image: every tile is the same width and height, laid out
 * left to right and top to bottom across any number of rows. There is no sidecar file (no Sparrow
 * XML, no JSON atlas); the grid position alone determines a tile's ID. The top-left cell is ID
 * {@code 1}, the next cell to the right is ID {@code 2}, and the numbering wraps to the start of
 * the next row once a row is full. ID {@code 0} is reserved to mean "empty" and never maps to a
 * region.
 *
 * <p>A 3-column, 2-row sheet therefore produces IDs 1 through 6:
 *
 * <pre>
 * +-----+-----+-----+
 * |  1  |  2  |  3  |   (row 0)
 * +-----+-----+-----+
 * |  4  |  5  |  6  |   (row 1)
 * +-----+-----+-----+
 * </pre>
 *
 * <p>Exporters often pad a sheet with a {@code margin} (empty pixels around the whole sheet) and
 * {@code spacing} (empty pixels between adjacent tiles) to stop neighboring tiles from bleeding
 * into each other when the GPU samples them. Both are supported; pass {@code 0} for each when the
 * sheet is a tight grid.
 *
 * <p>The number of columns and rows is derived from the texture size, so you never specify it by
 * hand. If the texture cannot be divided evenly for the given tile size, margin, and spacing, the
 * constructor throws {@link IllegalArgumentException} rather than silently producing misaligned
 * regions.
 *
 * <p><b>Example:</b>
 *
 * <pre>{@code
 * FlixelGraphic graphic = Flixel.ensureAssets().<FlixelGraphic>get("tiles/world.png").get();
 * FlixelTileset tileset = new FlixelTileset(graphic, 16, 16); // 16x16 tiles, no padding.
 * TextureRegion grass = tileset.getRegion(1); // Top-left tile.
 * }</pre>
 *
 * @see FlixelTilemap
 * @see FlixelTilemapLayer
 */
public final class FlixelTileset {

  private final int tileWidth;
  private final int tileHeight;
  private final int margin;
  private final int spacing;
  private final int columns;
  private final int rows;
  private final FlixelGraphic graphic;
  private final TextureRegion[] regions;

  /**
   * Creates a tileset from a tightly-packed grid sheet with no margin or spacing.
   *
   * @param graphic The reference-counted graphic wrapping the tile sheet texture. Retained for the
   *   lifetime of this tileset and released by {@link #dispose()}.
   * @param tileWidth The width of a single tile in pixels. Must be greater than {@code 0}.
   * @param tileHeight The height of a single tile in pixels. Must be greater than {@code 0}.
   * @throws IllegalArgumentException If the texture does not divide evenly into whole tiles.
   */
  public FlixelTileset(FlixelGraphic graphic, int tileWidth, int tileHeight) {
    this(graphic, tileWidth, tileHeight, 0, 0);
  }

  /**
   * Creates a tileset from a grid sheet that may have border margin and inter-tile spacing.
   *
   * @param graphic The reference-counted graphic wrapping the tile sheet texture. Retained for the
   *   lifetime of this tileset and released by {@link #dispose()}.
   * @param tileWidth The width of a single tile in pixels. Must be greater than {@code 0}.
   * @param tileHeight The height of a single tile in pixels. Must be greater than {@code 0}.
   * @param margin Empty pixels around the whole sheet border. Must not be negative.
   * @param spacing Empty pixels between adjacent tiles. Must not be negative.
   * @throws IllegalArgumentException If any argument is out of range or the texture does not divide
   *   evenly into whole tiles for the given tile size, margin, and spacing.
   */
  public FlixelTileset(FlixelGraphic graphic, int tileWidth, int tileHeight, int margin, int spacing) {
    if (graphic == null) {
      throw new IllegalArgumentException("graphic cannot be null.");
    }
    if (tileWidth <= 0 || tileHeight <= 0) {
      throw new IllegalArgumentException(
          "Tile size must be positive (got " + tileWidth + "x" + tileHeight + ").");
    }
    if (margin < 0 || spacing < 0) {
      throw new IllegalArgumentException(
          "Margin and spacing cannot be negative (got margin=" + margin + ", spacing=" + spacing + ").");
    }

    this.graphic = graphic.retain();
    this.tileWidth = tileWidth;
    this.tileHeight = tileHeight;
    this.margin = margin;
    this.spacing = spacing;

    Texture texture = graphic.getTexture();
    int texWidth = texture.getWidth();
    int texHeight = texture.getHeight();

    int cols = (texWidth - 2 * margin + spacing) / (tileWidth + spacing);
    int rowCount = (texHeight - 2 * margin + spacing) / (tileHeight + spacing);
    if (cols < 1 || rowCount < 1) {
      throw new IllegalArgumentException(
          "Texture " + texWidth + "x" + texHeight + " is too small for " + tileWidth + "x" + tileHeight
              + " tiles with margin=" + margin + ", spacing=" + spacing + ".");
    }
    int usedWidth = 2 * margin + cols * tileWidth + (cols - 1) * spacing;
    int usedHeight = 2 * margin + rowCount * tileHeight + (rowCount - 1) * spacing;
    if (usedWidth != texWidth || usedHeight != texHeight) {
      throw new IllegalArgumentException(
          "Texture " + texWidth + "x" + texHeight + " does not divide evenly into " + tileWidth + "x"
              + tileHeight + " tiles with margin=" + margin + ", spacing=" + spacing
              + " (tiles fill " + usedWidth + "x" + usedHeight + ").");
    }

    this.columns = cols;
    this.rows = rowCount;
    this.regions = new TextureRegion[cols * rowCount];
    for (int r = 0; r < rowCount; r++) {
      for (int c = 0; c < cols; c++) {
        int x = margin + c * (tileWidth + spacing);
        int y = margin + r * (tileHeight + spacing);
        regions[r * cols + c] = new TextureRegion(texture, x, y, tileWidth, tileHeight);
      }
    }
  }

  /**
   * Returns the region for the given 1-based tile ID, or {@code null} when the ID is {@code 0}
   * (empty) or falls outside the sheet. Never allocates.
   *
   * @param tileId The 1-based tile ID; {@code 0} means empty.
   * @return The tile's region, or {@code null} for empty or out-of-range IDs.
   */
  @Nullable
  public TextureRegion getRegion(int tileId) {
    if (tileId <= 0 || tileId > regions.length) {
      return null;
    }
    return regions[tileId - 1];
  }

  /**
   * Releases this tileset's hold on the backing graphic. Call this when the tileset is no longer
   * needed; {@link FlixelTilemap#destroy()} does it automatically for the tilesets its layers use.
   */
  public void dispose() {
    graphic.release();
  }

  /** Returns the number of tile columns detected in the sheet. */
  public int getColumns() {
    return columns;
  }

  /** Returns the number of tile rows detected in the sheet. */
  public int getRows() {
    return rows;
  }

  /** Returns the total number of tiles in the sheet, which is also the highest valid tile ID. */
  public int getTileCount() {
    return regions.length;
  }

  /** Returns the width of a single tile in pixels. */
  public int getTileWidth() {
    return tileWidth;
  }

  /** Returns the height of a single tile in pixels. */
  public int getTileHeight() {
    return tileHeight;
  }

  /** Returns the border margin in pixels applied around the sheet. */
  public int getMargin() {
    return margin;
  }

  /** Returns the spacing in pixels between adjacent tiles. */
  public int getSpacing() {
    return spacing;
  }

  /** Returns the reference-counted graphic backing this tileset. */
  public FlixelGraphic getGraphic() {
    return graphic;
  }
}
