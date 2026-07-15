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

import com.badlogic.gdx.graphics.g2d.TextureRegion;

import org.flixelgdx.GdxHeadlessExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies that {@link FlixelTileset} slices a grid sheet into the correct regions and rejects
 * textures that do not divide evenly.
 */
@ExtendWith(GdxHeadlessExtension.class)
public class FlixelTilesetTest {

  @Test
  public void singleRow_noPadding_slicesLeftToRight() {
    FlixelTileset tileset = TilemapTestSupport.newTileset(48, 16, 16, 16, 0, 0);

    assertEquals(3, tileset.getColumns());
    assertEquals(1, tileset.getRows());
    assertEquals(3, tileset.getTileCount());
    assertNull(tileset.getRegion(0), "ID 0 must be empty.");
    assertNotNull(tileset.getRegion(1));
    assertNotNull(tileset.getRegion(3));
    assertNull(tileset.getRegion(4), "IDs past the last tile must be null.");

    assertEquals(0, tileset.getRegion(1).getRegionX());
    assertEquals(32, tileset.getRegion(3).getRegionX());
  }

  @Test
  public void multiRow_noPadding_numbersAcrossRows() {
    FlixelTileset tileset = TilemapTestSupport.newTileset(48, 32, 16, 16, 0, 0);

    assertEquals(3, tileset.getColumns());
    assertEquals(2, tileset.getRows());
    assertEquals(6, tileset.getTileCount());

    // ID 4 is the first tile of the second row.
    TextureRegion four = tileset.getRegion(4);
    assertEquals(0, four.getRegionX());
    assertEquals(16, four.getRegionY());

    // ID 6 is the last tile: bottom-right cell.
    TextureRegion six = tileset.getRegion(6);
    assertEquals(32, six.getRegionX());
    assertEquals(16, six.getRegionY());
  }

  @Test
  public void marginAndSpacing_offsetRegions() {
    // 2x2 tiles of 16px, 1px margin, 2px spacing -> 1 + 16 + 2 + 16 + 1 = 36 on each axis.
    FlixelTileset tileset = TilemapTestSupport.newTileset(36, 36, 16, 16, 1, 2);

    assertEquals(2, tileset.getColumns());
    assertEquals(2, tileset.getRows());
    assertEquals(4, tileset.getTileCount());

    assertEquals(1, tileset.getRegion(1).getRegionX());
    assertEquals(1, tileset.getRegion(1).getRegionY());
    // Second column starts after margin + tile + spacing = 1 + 16 + 2 = 19.
    assertEquals(19, tileset.getRegion(2).getRegionX());
    // Second row starts at the same offset on Y.
    assertEquals(19, tileset.getRegion(3).getRegionY());
  }

  @Test
  public void unevenTexture_throws() {
    // 40px wide only fits 2 whole 16px tiles (32px), leaving 8px -> does not divide evenly.
    assertThrows(
        IllegalArgumentException.class,
        () -> TilemapTestSupport.newTileset(40, 16, 16, 16, 0, 0));
  }

  @Test
  public void textureSmallerThanTile_throws() {
    assertThrows(
        IllegalArgumentException.class,
        () -> TilemapTestSupport.newTileset(8, 8, 16, 16, 0, 0));
  }
}
