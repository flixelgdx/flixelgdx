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

import org.flixelgdx.FlixelCamera;
import org.flixelgdx.FlixelObject;
import org.flixelgdx.GdxHeadlessExtension;
import org.flixelgdx.util.FlixelDirectionFlags;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the recycled-grid ring buffer, tile lookup and mutation, solid collision, interaction,
 * and tile behavior callbacks of {@link FlixelTilemap}.
 */
@ExtendWith(GdxHeadlessExtension.class)
public class FlixelTilemapTest {

  private static FlixelTilemapLayer addFilledLayer(FlixelTilemap map, int fillId) {
    int[] tiles = new int[map.getMapWidth() * map.getMapHeight()];
    for (int i = 0; i < tiles.length; i++) {
      tiles[i] = fillId;
    }
    return map.addLayer("layer", tiles, TilemapTestSupport.newTileset(48, 48, 16, 16, 0, 0));
  }

  @Test
  public void ringBuffer_advancesOriginsWithScroll() {
    FlixelTilemap map = new FlixelTilemap(1000, 10, 16, 16);
    FlixelTilemapLayer layer = addFilledLayer(map, 1);
    FlixelCamera cam = new FlixelCamera(48, 48);

    // ceil(48 / 16) + 2 = 5 slots on each axis.
    cam.scrollX = 0f;
    cam.scrollY = 0f;
    map.prepareLayer(layer, cam);
    assertEquals(5, map.getGridCols());
    assertEquals(5, map.getGridRows());
    // origin = floor(scroll / tile) - 1 = floor(0) - 1 = -1.
    assertEquals(-1, layer.mapOriginCol);
    assertEquals(0, layer.ringOriginX);

    // Scroll right one tile: origin advances by 1, ring head rotates by 1.
    cam.scrollX = 16f;
    map.prepareLayer(layer, cam);
    assertEquals(0, layer.mapOriginCol);
    assertEquals(1, layer.ringOriginX);

    // Scroll right two more tiles.
    cam.scrollX = 48f;
    map.prepareLayer(layer, cam);
    assertEquals(2, layer.mapOriginCol);
    assertEquals(3, layer.ringOriginX);

    // Scroll all the way back left: ring head wraps back to 0.
    cam.scrollX = 0f;
    map.prepareLayer(layer, cam);
    assertEquals(-1, layer.mapOriginCol);
    assertEquals(0, layer.ringOriginX);
  }

  @Test
  public void ringBuffer_largeJumpTriggersFullRebuild() {
    FlixelTilemap map = new FlixelTilemap(2000, 10, 16, 16);
    FlixelTilemapLayer layer = addFilledLayer(map, 1);
    FlixelCamera cam = new FlixelCamera(48, 48);

    cam.scrollX = 0f;
    map.prepareLayer(layer, cam);

    // A jump larger than the grid width rebuilds from scratch and resets the ring head.
    cam.scrollX = 16000f;
    map.prepareLayer(layer, cam);
    assertEquals(999, layer.mapOriginCol);
    assertEquals(0, layer.ringOriginX);
  }

  @Test
  public void tileLookup_returnsExpectedIds() {
    FlixelTilemap map = new FlixelTilemap(3, 3, 16, 16);
    int[] tiles = { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
    map.addLayer("ground", tiles, TilemapTestSupport.newTileset(48, 48, 16, 16, 0, 0));

    assertEquals(1, map.getTileAtMapPos(0, 0, 0));
    assertEquals(3, map.getTileAtMapPos(2, 0, 0));
    assertEquals(4, map.getTileAtMapPos(0, 1, 0));
    assertEquals(9, map.getTileAtMapPos(2, 2, 0));
    assertEquals(0, map.getTileAtMapPos(3, 0, 0), "Out-of-range column returns 0.");
    assertEquals(0, map.getTileAtMapPos(-1, 0, 0), "Negative column returns 0.");

    // World-space lookup: (17, 1) lands in column 1, row 0 -> tile 2.
    assertEquals(2, map.getTileAtWorldPos(17f, 1f, 0));
  }

  @Test
  public void setTile_updatesDataAndVisibleGrid() {
    FlixelTilemap map = new FlixelTilemap(3, 3, 16, 16);
    int[] tiles = { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
    FlixelTilemapLayer layer =
        map.addLayer("ground", tiles, TilemapTestSupport.newTileset(48, 48, 16, 16, 0, 0));
    FlixelCamera cam = new FlixelCamera(48, 48);
    cam.scrollX = 0f;
    cam.scrollY = 0f;
    map.prepareLayer(layer, cam);

    // Origins are -1, so map tile (1, 1) sits at visible offset (2, 2) -> physical slot (2, 2).
    assertNotNull(layer.grid[2][2]);

    // Clearing the tile updates the raw data and the visible grid slot.
    map.setTileAtMapPos(1, 1, 0, 0);
    assertEquals(0, map.getTileAtMapPos(1, 1, 0));
    assertNull(layer.grid[2][2]);

    // Setting a new tile id repaints the slot with that tile's region.
    map.setTileAtMapPos(1, 1, 0, 9);
    assertSame(layer.getTileset().getRegion(9), layer.grid[2][2]);
  }

  @Test
  public void isSolidAt_respectsSolidFlags() {
    FlixelTilemap map = new FlixelTilemap(3, 3, 16, 16);
    int[] tiles = new int[9];
    tiles[1 * 3 + 1] = 1; // Solid tile at (1, 1).
    FlixelTilemapLayer layer =
        map.addLayer("ground", tiles, TilemapTestSupport.newTileset(48, 48, 16, 16, 0, 0));
    layer.setSolid(1);

    // Tile (1, 1) spans world x/y 16..32.
    assertTrue(map.isSolidAt(20f, 20f));
    assertFalse(map.isSolidAt(4f, 4f), "Empty tile (0, 0) is not solid.");
  }

  @Test
  public void collide_pushesObjectOutOfSolidTile() {
    FlixelTilemap map = new FlixelTilemap(5, 5, 16, 16);
    int[] tiles = new int[25];
    tiles[2 * 5 + 2] = 1; // Solid tile at (2, 2), world rect 32..48.
    FlixelTilemapLayer layer =
        map.addLayer("ground", tiles, TilemapTestSupport.newTileset(48, 48, 16, 16, 0, 0));
    layer.setSolid(1);

    // Object starts above the tile and moves into it, giving separation a movement delta to work with.
    FlixelObject obj = new FlixelObject(34f, 20f, 8f, 8f);
    obj.setVelocityY(140f);
    obj.update(0.1f); // lastY = 20, y moves to ~34, overlapping the tile.

    boolean collided = map.collide(obj);
    assertTrue(collided);
    assertEquals(24f, obj.getY(), 0.001f, "Object is pushed back out of the tile.");
    assertEquals(0f, obj.getVelocityY(), 0.001f, "Velocity into the tile is cancelled.");
    assertTrue(
        obj.isTouching(FlixelDirectionFlags.UP) || obj.isTouching(FlixelDirectionFlags.DOWN),
        "A vertical touching flag is set after separation.");
  }

  @Test
  public void layerBrightness_setsTintChannels() {
    FlixelTilemap map = new FlixelTilemap(3, 3, 16, 16);
    FlixelTilemapLayer layer =
        map.addLayer("bg", new int[9], TilemapTestSupport.newTileset(48, 48, 16, 16, 0, 0));

    // Default tint is opaque white (no change to the art).
    assertEquals(1f, layer.getTint().r, 0.001f);
    assertEquals(1f, layer.getTint().a, 0.001f);

    layer.setBrightness(0.6f);
    assertEquals(0.6f, layer.getTint().r, 0.001f);
    assertEquals(0.6f, layer.getTint().g, 0.001f);
    assertEquals(0.6f, layer.getTint().b, 0.001f);
    assertEquals(1f, layer.getTint().a, 0.001f, "Brightness leaves alpha untouched.");
  }

  @Test
  public void interact_firesBehaviorAndBreaksTile() {
    FlixelTilemap map = new FlixelTilemap(3, 3, 16, 16);
    int[] tiles = new int[9];
    tiles[1 * 3 + 1] = 3; // Breakable tile at (1, 1).
    FlixelTilemapLayer layer =
        map.addLayer("ground", tiles, TilemapTestSupport.newTileset(48, 48, 16, 16, 0, 0));
    layer.setBehavior(3, new BreakableTile());

    // World point (20, 20) is inside tile (1, 1).
    assertTrue(map.interact(20f, 20f));
    assertEquals(0, map.getTileAtMapPos(1, 1, 0), "Interacting broke the tile.");
    // Interacting again finds no behavior there anymore.
    assertFalse(map.interact(20f, 20f));
  }

  @Test
  public void collide_firesEnterTouchAndLeaveCallbacks() {
    FlixelTilemap map = new FlixelTilemap(3, 3, 16, 16);
    int[] tiles = new int[9];
    tiles[1 * 3 + 1] = 2; // Behavior tile at (1, 1).
    FlixelTilemapLayer layer =
        map.addLayer("ground", tiles, TilemapTestSupport.newTileset(48, 48, 16, 16, 0, 0));
    CountingBehavior counting = new CountingBehavior();
    layer.setBehavior(2, counting);

    // Object sits fully inside tile (1, 1) (world rect 16..32).
    FlixelObject obj = new FlixelObject(18f, 18f, 8f, 8f);

    map.collide(obj);
    assertEquals(1, counting.enters);
    assertEquals(1, counting.touches);
    assertEquals(0, counting.leaves);

    // Still overlapping: another enter is not fired, but touch is.
    map.collide(obj);
    assertEquals(1, counting.enters);
    assertEquals(2, counting.touches);
    assertEquals(0, counting.leaves);

    // Move the object off the tile: leave fires once.
    obj.setPosition(0f, 0f);
    map.collide(obj);
    assertEquals(1, counting.enters);
    assertEquals(2, counting.touches);
    assertEquals(1, counting.leaves);
  }

  /** Test behavior that removes its tile when interacted with. */
  private static final class BreakableTile extends FlixelTileBehavior {

    @Override
    public void onInteract(FlixelTilemap map, int col, int row, FlixelTilemapLayer layer) {
      map.setTileAtMapPos(col, row, map.getLayerIndex(layer), 0);
    }
  }

  /** Test behavior that counts enter, touch, and leave events. */
  private static final class CountingBehavior extends FlixelTileBehavior {

    int enters;
    int touches;
    int leaves;

    @Override
    public void onEnter(FlixelTilemap map, FlixelObject obj, int col, int row, FlixelTilemapLayer layer) {
      enters++;
    }

    @Override
    public void onTouch(FlixelTilemap map, FlixelObject obj, int col, int row, FlixelTilemapLayer layer) {
      touches++;
    }

    @Override
    public void onLeave(FlixelTilemap map, FlixelObject obj, int col, int row, FlixelTilemapLayer layer) {
      leaves++;
    }
  }
}
