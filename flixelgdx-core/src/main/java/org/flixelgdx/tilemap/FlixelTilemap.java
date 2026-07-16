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

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.ObjectMap;

import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelCamera;
import org.flixelgdx.FlixelObject;
import org.flixelgdx.graphics.FlixelBatch;
import org.flixelgdx.util.FlixelDirectionFlags;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A memory-flat tilemap drawn with a recycled grid of tile slots instead of one object per tile.
 *
 * <p>Rather than wrapping libGDX's heavyweight {@code TiledMap}, this class keeps a small pool of
 * visible tile slots sized to the camera view plus a one-tile margin on each edge. That pool is
 * allocated once. As the camera scrolls, only the columns and rows that newly enter the view are
 * refilled - the tiles that scroll off one edge are recycled and repositioned on the opposite
 * edge with new data. The result is zero per-frame allocation and constant memory use no matter
 * how large the map is. Think of it like a conveyor belt of tiles: the belt is a fixed length, and
 * as it moves, tiles that fall off the back are placed onto the front with fresh art.
 *
 * <p>A tilemap holds one or more named {@link FlixelTilemapLayer}s. Each layer has its own tile
 * data, its own {@link FlixelTileset}, and its own parallax scroll factor, so you can stack a
 * slow-drifting background under a foreground that moves with the player. Tiles use 1-based IDs;
 * ID {@code 0} is always empty and draws nothing.
 *
 * <p><b>Typical setup:</b>
 *
 * <pre>{@code
 * FlixelGraphic graphic = Flixel.ensureAssets().<FlixelGraphic>get("tiles/world.png").get();
 * FlixelTileset tileset = new FlixelTileset(graphic, 16, 16);
 *
 * int[] ground = { ... }; // mapWidth * mapHeight entries, row-major, 1-based IDs.
 * FlixelTilemap map = new FlixelTilemap(mapWidth, mapHeight, 16, 16);
 * FlixelTilemapLayer layer = map.addLayer("ground", ground, tileset);
 * layer.setSolid(1, 2, 3); // Tiles 1-3 block movement.
 * add(map);                // Add to a state like any other object.
 *
 * // Somewhere in your state's update:
 * map.collide(player);              // Push the player out of solid tiles.
 * map.clampCameraToMap(Flixel.camera); // Keep the camera inside the level.
 * }</pre>
 *
 * <h2>Infinite looping</h2>
 * Set {@link #loopX} and/or {@link #loopY} to tile the map forever on that axis. The two axes are
 * independent, so {@code loopX = true} with {@code loopY = false} scrolls horizontally forever
 * while still clamping vertically. {@link #clampCameraToMap(FlixelCamera)} automatically skips any
 * axis that is set to loop.
 *
 * @see FlixelTilemapLayer
 * @see FlixelTileset
 * @see FlixelTileBehavior
 */
public class FlixelTilemap extends FlixelObject {

  private static final int CONTACT_COORD_BITS = 14;
  private static final int CONTACT_COORD_MASK = (1 << CONTACT_COORD_BITS) - 1;
  private static final int CONTACT_LAYER_SHIFT = 28;

  private final int mapWidth;
  private final int mapHeight;
  private final int tileWidth;
  private final int tileHeight;
  private int gridCols;
  private int gridRows;
  private final Array<FlixelTilemapLayer> layers;

  /** Previous-frame tile contacts per colliding object, used to fire behavior enter/leave events. */
  @Nullable
  private ObjectMap<FlixelObject, IntArray> contacts;

  /** Reusable scratch set of the current frame's contacts, to avoid per-frame allocation. */
  @Nullable
  private IntArray scratchContacts;

  /** Reusable immovable stand-in for a solid tile during collision. Allocated on first collide. */
  @Nullable
  private TileCollider tileCollider;

  /** Whether the map tiles infinitely on the X axis. Set before the first draw for best results. */
  public boolean loopX;

  /** Whether the map tiles infinitely on the Y axis. Set before the first draw for best results. */
  public boolean loopY;

  /**
   * Creates an empty tilemap. Add tile data with
   * {@link #addLayer(String, int[], FlixelTileset)}.
   *
   * @param mapWidth The map width in tiles. Must be greater than {@code 0}.
   * @param mapHeight The map height in tiles. Must be greater than {@code 0}.
   * @param tileWidth The tile width in pixels. Must be greater than {@code 0}.
   * @param tileHeight The tile height in pixels. Must be greater than {@code 0}.
   * @throws IllegalArgumentException If any dimension is not positive.
   */
  public FlixelTilemap(int mapWidth, int mapHeight, int tileWidth, int tileHeight) {
    super(0f, 0f, mapWidth * (float) tileWidth, mapHeight * (float) tileHeight);
    if (mapWidth <= 0 || mapHeight <= 0 || tileWidth <= 0 || tileHeight <= 0) {
      throw new IllegalArgumentException(
          "Map and tile dimensions must be positive (got map " + mapWidth + "x" + mapHeight
              + ", tile " + tileWidth + "x" + tileHeight + ").");
    }
    this.mapWidth = mapWidth;
    this.mapHeight = mapHeight;
    this.tileWidth = tileWidth;
    this.tileHeight = tileHeight;
    this.layers = new Array<>(FlixelTilemapLayer[]::new);
  }

  /**
   * Updates every registered {@link FlixelTileBehavior} once per frame.
   *
   * @param elapsed Seconds elapsed since the last frame.
   */
  @Override
  public void update(float elapsed) {
    super.update(elapsed);
    for (int li = 0; li < layers.size; li++) {
      FlixelTileBehavior[] behaviors = layers.get(li).behaviors;
      if (behaviors == null) {
        continue;
      }
      for (int id = 0; id < behaviors.length; id++) {
        FlixelTileBehavior behavior = behaviors[id];
        if (behavior != null) {
          behavior.update(elapsed);
        }
      }
    }
  }

  /**
   * Syncs each visible layer's recycled grid to the current camera scroll, then draws it. Each
   * layer is drawn through its own {@link FlixelTilemapLayer#getTint() tint}, so background layers
   * can be darkened or faded independently.
   *
   * @param batch The batch to draw into.
   */
  @Override
  public void draw(@NotNull FlixelBatch batch) {
    if (!visible || !isOnDrawCamera()) {
      return;
    }
    FlixelCamera cam = resolveCamera();
    if (cam == null) {
      return;
    }
    for (int li = 0; li < layers.size; li++) {
      FlixelTilemapLayer layer = layers.get(li);
      if (!layer.visible) {
        continue;
      }
      prepareLayer(layer, cam);
      TextureRegion[][] grid = layer.grid;
      if (grid == null) {
        continue;
      }
      batch.setColor(layer.tint);
      for (int gx = 0; gx < gridCols; gx++) {
        int physicalCol = (layer.ringOriginX + gx) % gridCols;
        float worldX = getX() + (layer.mapOriginCol + gx) * (float) tileWidth;
        float viewX = cam.worldToViewX(worldX, layer.scrollFactorX);
        for (int gy = 0; gy < gridRows; gy++) {
          int physicalRow = (layer.ringOriginY + gy) % gridRows;
          TextureRegion region = grid[physicalCol][physicalRow];
          if (region == null) {
            continue;
          }
          float worldY = getY() + (layer.mapOriginRow + gy) * (float) tileHeight;
          float viewY = cam.worldToViewY(worldY, layer.scrollFactorY);
          batch.draw(region, viewX, viewY, tileWidth, tileHeight);
        }
      }
    }
    batch.setColor(Color.WHITE);
  }

  /**
   * Adds a layer to the top of the draw order.
   *
   * @param name The layer's name, used for {@link #getLayer(String)} lookups.
   * @param tiles The flat, row-major tile IDs. Length must equal {@code mapWidth * mapHeight}. The
   *   array is used directly (not copied), so later edits to it are reflected by the map.
   * @param tileset The tileset the layer draws its tiles from.
   * @return The created layer, for further configuration.
   * @throws IllegalArgumentException If {@code tiles} is the wrong length.
   */
  public FlixelTilemapLayer addLayer(String name, int[] tiles, FlixelTileset tileset) {
    if (tiles == null || tiles.length != mapWidth * mapHeight) {
      throw new IllegalArgumentException(
          "Tile array length must be mapWidth * mapHeight (" + (mapWidth * mapHeight) + "), got "
              + (tiles == null ? "null" : tiles.length) + ".");
    }
    if (tileset == null) {
      throw new IllegalArgumentException("tileset cannot be null.");
    }
    FlixelTilemapLayer layer = new FlixelTilemapLayer(name, tiles, tileset);
    layers.add(layer);
    return layer;
  }

  /**
   * Returns the layer at the given index, or {@code null} if the index is out of range.
   *
   * @param index The layer index, where {@code 0} is the bottom of the draw order.
   * @return The layer, or {@code null}.
   */
  @Nullable
  public FlixelTilemapLayer getLayer(int index) {
    if (index < 0 || index >= layers.size) {
      return null;
    }
    return layers.get(index);
  }

  /**
   * Returns the first layer with the given name, or {@code null} if none matches.
   *
   * @param name The layer name to search for.
   * @return The matching layer, or {@code null}.
   */
  @Nullable
  public FlixelTilemapLayer getLayer(String name) {
    for (int i = 0; i < layers.size; i++) {
      FlixelTilemapLayer layer = layers.get(i);
      if (name == null ? layer.name == null : name.equals(layer.name)) {
        return layer;
      }
    }
    return null;
  }

  /**
   * Returns the index of the given layer, or {@code -1} if it is not part of this map.
   *
   * @param layer The layer to locate.
   * @return The layer's index, or {@code -1}.
   */
  public int getLayerIndex(FlixelTilemapLayer layer) {
    return layers.indexOf(layer, true);
  }

  /**
   * Returns the tile ID at the given map position on the given layer. Respects looping; returns
   * {@code 0} for positions outside a non-looping map.
   *
   * @param mapCol The tile column.
   * @param mapRow The tile row.
   * @param layerIndex The layer index.
   * @return The 1-based tile ID, or {@code 0} for empty or out-of-range positions.
   */
  public int getTileAtMapPos(int mapCol, int mapRow, int layerIndex) {
    if (layerIndex < 0 || layerIndex >= layers.size) {
      return 0;
    }
    int col = mapCol;
    int row = mapRow;
    if (loopX) {
      col = ((col % mapWidth) + mapWidth) % mapWidth;
    } else if (col < 0 || col >= mapWidth) {
      return 0;
    }
    if (loopY) {
      row = ((row % mapHeight) + mapHeight) % mapHeight;
    } else if (row < 0 || row >= mapHeight) {
      return 0;
    }
    return layers.get(layerIndex).tiles[row * mapWidth + col];
  }

  /**
   * Returns the tile ID at the given world position on the given layer.
   *
   * @param worldX World-space X.
   * @param worldY World-space Y.
   * @param layerIndex The layer index.
   * @return The 1-based tile ID, or {@code 0} for empty or out-of-range positions.
   */
  public int getTileAtWorldPos(float worldX, float worldY, int layerIndex) {
    int col = (int) Math.floor((worldX - getX()) / tileWidth);
    int row = (int) Math.floor((worldY - getY()) / tileHeight);
    return getTileAtMapPos(col, row, layerIndex);
  }

  /**
   * Sets the tile ID at the given map position on the given layer. If the position is currently
   * inside the visible grid, the change appears on the next frame without a full rebuild.
   *
   * @param mapCol The tile column.
   * @param mapRow The tile row.
   * @param layerIndex The layer index.
   * @param tileId The new 1-based tile ID, or {@code 0} to clear the tile.
   */
  public void setTileAtMapPos(int mapCol, int mapRow, int layerIndex, int tileId) {
    if (layerIndex < 0 || layerIndex >= layers.size
        || mapCol < 0 || mapRow < 0 || mapCol >= mapWidth || mapRow >= mapHeight) {
      return;
    }
    FlixelTilemapLayer layer = layers.get(layerIndex);
    layer.tiles[mapRow * mapWidth + mapCol] = tileId;

    TextureRegion[][] grid = layer.grid;
    if (grid == null || gridCols == 0) {
      return;
    }
    for (int gx = 0; gx < gridCols; gx++) {
      if (normalizeCol(layer.mapOriginCol + gx) != mapCol) {
        continue;
      }
      int physicalCol = (layer.ringOriginX + gx) % gridCols;
      for (int gy = 0; gy < gridRows; gy++) {
        if (normalizeRow(layer.mapOriginRow + gy) != mapRow) {
          continue;
        }
        int physicalRow = (layer.ringOriginY + gy) % gridRows;
        grid[physicalCol][physicalRow] = tileId <= 0 ? null : layer.tileset.getRegion(tileId);
      }
    }
  }

  /**
   * Returns whether any layer has a solid tile at the given world position.
   *
   * @param worldX World-space X.
   * @param worldY World-space Y.
   * @return {@code true} if a solid tile occupies that position on any layer.
   */
  public boolean isSolidAt(float worldX, float worldY) {
    int col = (int) Math.floor((worldX - getX()) / tileWidth);
    int row = (int) Math.floor((worldY - getY()) / tileHeight);
    for (int i = 0; i < layers.size; i++) {
      if (layers.get(i).isSolidAt(col, row, mapWidth, mapHeight)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Pushes an object out of any solid tiles it overlaps and fires tile behavior enter, touch, and
   * leave callbacks for the tiles it is touching.
   *
   * <p>Separation reuses the framework's {@link FlixelObject#separate(FlixelObject, FlixelObject)}
   * logic against each solid tile, so the object's {@code touching} flags are set exactly as they
   * would be against a solid object. That lets ground checks such as
   * {@code obj.isTouching(FlixelDirectionFlags.DOWN)} work after a collide call.
   *
   * <p>Behavior enter and leave events are tracked per object across frames. This tracking supports
   * maps up to {@value #CONTACT_COORD_MASK} tiles on each axis and up to 15 layers; beyond that,
   * enter and leave events may occasionally be missed, though separation is unaffected.
   *
   * @param obj The object to collide against the map.
   * @return {@code true} if the object was separated from at least one solid tile.
   */
  public boolean collide(FlixelObject obj) {
    if (obj == null) {
      return false;
    }
    if (tileCollider == null) {
      tileCollider = new TileCollider();
    }

    float objX = obj.getX();
    float objY = obj.getY();
    int minCol = (int) Math.floor((objX - getX()) / tileWidth);
    int maxCol = (int) Math.floor((objX + obj.getWidth() - getX()) / tileWidth);
    int minRow = (int) Math.floor((objY - getY()) / tileHeight);
    int maxRow = (int) Math.floor((objY + obj.getHeight() - getY()) / tileHeight);

    boolean trackContacts = layersHaveBehaviors();
    IntArray previous = (trackContacts && contacts != null) ? contacts.get(obj) : null;
    if (trackContacts) {
      if (scratchContacts == null) {
        scratchContacts = new IntArray();
      }
      scratchContacts.clear();
    }

    boolean separated = false;
    for (int li = 0; li < layers.size; li++) {
      FlixelTilemapLayer layer = layers.get(li);
      boolean hasSolids = layer.solidTiles != null;
      boolean hasBehaviors = layer.behaviors != null;
      if (!hasSolids && !hasBehaviors) {
        continue;
      }
      for (int row = minRow; row <= maxRow; row++) {
        for (int col = minCol; col <= maxCol; col++) {
          int id = getTileAtMapPos(col, row, li);
          if (id <= 0) {
            continue;
          }
          if (hasSolids && layer.isSolidAt(col, row, mapWidth, mapHeight)) {
            placeTileCollider(col, row);
            if (FlixelObject.separate(obj, tileCollider)) {
              separated = true;
            }
          }
          if (trackContacts && hasBehaviors && layer.getBehavior(id) != null
              && objectOverlapsTile(obj, col, row)) {
            scratchContacts.add(packContact(li, normalizeCol(col), normalizeRow(row)));
          }
        }
      }
    }

    if (trackContacts) {
      fireBehaviorContacts(obj, previous);
    }
    return separated;
  }

  /**
   * Interacts with the top-most tile at the given world position that has a behavior registered,
   * firing its {@link FlixelTileBehavior#onInteract} callback. Call this yourself in response to a
   * mouse click, a button press, or any other explicit interaction.
   *
   * @param worldX World-space X.
   * @param worldY World-space Y.
   * @return {@code true} if a behavior was found and fired.
   */
  public boolean interact(float worldX, float worldY) {
    int col = (int) Math.floor((worldX - getX()) / tileWidth);
    int row = (int) Math.floor((worldY - getY()) / tileHeight);
    for (int li = layers.size - 1; li >= 0; li--) {
      FlixelTilemapLayer layer = layers.get(li);
      if (layer.behaviors == null) {
        continue;
      }
      int id = getTileAtMapPos(col, row, li);
      if (id <= 0) {
        continue;
      }
      FlixelTileBehavior behavior = layer.getBehavior(id);
      if (behavior != null) {
        behavior.onInteract(this, normalizeCol(col), normalizeRow(row), layer);
        return true;
      }
    }
    return false;
  }

  /**
   * Locks a camera's scroll bounds to this map's extent. Axes set to loop are left untouched so the
   * camera can scroll them forever.
   *
   * <p>For a map that loops horizontally but not vertically ({@code loopX = true},
   * {@code loopY = false}), this clamps only the Y axis, letting the camera pan sideways without
   * limit while still keeping the top and bottom of the level in view.
   *
   * @param cam The camera to bound.
   */
  public void clampCameraToMap(FlixelCamera cam) {
    if (cam == null) {
      return;
    }
    if (!loopX) {
      cam.minScrollX = getX();
      cam.maxScrollX = getX() + mapWidth * (float) tileWidth;
    }
    if (!loopY) {
      cam.minScrollY = getY();
      cam.maxScrollY = getY() + mapHeight * (float) tileHeight;
    }
  }

  @Override
  public void destroy() {
    Array<FlixelTileBehavior> destroyedBehaviors = new Array<>();
    Array<FlixelTileset> disposedTilesets = new Array<>();
    for (int li = 0; li < layers.size; li++) {
      FlixelTilemapLayer layer = layers.get(li);
      if (layer.behaviors != null) {
        for (int id = 0; id < layer.behaviors.length; id++) {
          FlixelTileBehavior behavior = layer.behaviors[id];
          if (behavior != null && !containsIdentity(destroyedBehaviors, behavior)) {
            behavior.destroy();
            destroyedBehaviors.add(behavior);
          }
        }
      }
      FlixelTileset tileset = layer.tileset;
      if (tileset != null && !containsIdentity(disposedTilesets, tileset)) {
        tileset.dispose();
        disposedTilesets.add(tileset);
      }
    }
    layers.clear();
    if (contacts != null) {
      contacts.clear();
    }
    tileCollider = null;
    super.destroy();
  }

  /**
   * Ensures the given layer's recycled grid exists and is synced to the camera. Package-private so
   * tests can drive the ring buffer without a real draw pass.
   *
   * @param layer The layer to prepare.
   * @param cam The camera whose scroll drives the sync.
   */
  void prepareLayer(FlixelTilemapLayer layer, FlixelCamera cam) {
    ensureGridSizing(cam);
    if (layer.grid == null) {
      layer.grid = new TextureRegion[gridCols][gridRows];
      layer.ringOriginX = 0;
      layer.ringOriginY = 0;
      layer.mapOriginCol = computeOriginCol(cam, layer);
      layer.mapOriginRow = computeOriginRow(cam, layer);
      fillGrid(layer);
      layer.lastScrollX = cam.scrollX;
      layer.lastScrollY = cam.scrollY;
    } else {
      syncLayer(layer, cam);
    }
  }

  /**
   * Advances the given layer's ring buffer to match the camera scroll, refilling only the columns
   * and rows that newly entered the view. Package-private for testing.
   *
   * @param layer The layer to sync. Its grid must already be allocated.
   * @param cam The camera whose scroll drives the sync.
   */
  void syncLayer(FlixelTilemapLayer layer, FlixelCamera cam) {
    int newOriginCol = computeOriginCol(cam, layer);
    int newOriginRow = computeOriginRow(cam, layer);
    int deltaCol = newOriginCol - layer.mapOriginCol;
    int deltaRow = newOriginRow - layer.mapOriginRow;

    if (deltaCol == 0 && deltaRow == 0) {
      layer.lastScrollX = cam.scrollX;
      layer.lastScrollY = cam.scrollY;
      return;
    }

    if (Math.abs(deltaCol) >= gridCols || Math.abs(deltaRow) >= gridRows) {
      layer.mapOriginCol = newOriginCol;
      layer.mapOriginRow = newOriginRow;
      fillGrid(layer);
      layer.lastScrollX = cam.scrollX;
      layer.lastScrollY = cam.scrollY;
      return;
    }

    for (int i = 0; i < deltaCol; i++) {
      shiftColumnRight(layer);
    }
    for (int i = 0; i < -deltaCol; i++) {
      shiftColumnLeft(layer);
    }
    for (int i = 0; i < deltaRow; i++) {
      shiftRowDown(layer);
    }
    for (int i = 0; i < -deltaRow; i++) {
      shiftRowUp(layer);
    }
    layer.lastScrollX = cam.scrollX;
    layer.lastScrollY = cam.scrollY;
  }

  private void shiftColumnRight(FlixelTilemapLayer layer) {
    int physicalCol = layer.ringOriginX;
    int newMapCol = layer.mapOriginCol + gridCols;
    fillPhysicalColumn(layer, physicalCol, newMapCol);
    layer.ringOriginX = (layer.ringOriginX + 1) % gridCols;
    layer.mapOriginCol++;
  }

  private void shiftColumnLeft(FlixelTilemapLayer layer) {
    layer.ringOriginX = (layer.ringOriginX - 1 + gridCols) % gridCols;
    layer.mapOriginCol--;
    fillPhysicalColumn(layer, layer.ringOriginX, layer.mapOriginCol);
  }

  private void shiftRowDown(FlixelTilemapLayer layer) {
    int physicalRow = layer.ringOriginY;
    int newMapRow = layer.mapOriginRow + gridRows;
    fillPhysicalRow(layer, physicalRow, newMapRow);
    layer.ringOriginY = (layer.ringOriginY + 1) % gridRows;
    layer.mapOriginRow++;
  }

  private void shiftRowUp(FlixelTilemapLayer layer) {
    layer.ringOriginY = (layer.ringOriginY - 1 + gridRows) % gridRows;
    layer.mapOriginRow--;
    fillPhysicalRow(layer, layer.ringOriginY, layer.mapOriginRow);
  }

  private void fillPhysicalColumn(FlixelTilemapLayer layer, int physicalCol, int mapCol) {
    for (int gy = 0; gy < gridRows; gy++) {
      int physicalRow = (layer.ringOriginY + gy) % gridRows;
      layer.grid[physicalCol][physicalRow] = tileRegionAt(layer, mapCol, layer.mapOriginRow + gy);
    }
  }

  private void fillPhysicalRow(FlixelTilemapLayer layer, int physicalRow, int mapRow) {
    for (int gx = 0; gx < gridCols; gx++) {
      int physicalCol = (layer.ringOriginX + gx) % gridCols;
      layer.grid[physicalCol][physicalRow] = tileRegionAt(layer, layer.mapOriginCol + gx, mapRow);
    }
  }

  private void fillGrid(FlixelTilemapLayer layer) {
    layer.ringOriginX = 0;
    layer.ringOriginY = 0;
    for (int gx = 0; gx < gridCols; gx++) {
      for (int gy = 0; gy < gridRows; gy++) {
        layer.grid[gx][gy] = tileRegionAt(layer, layer.mapOriginCol + gx, layer.mapOriginRow + gy);
      }
    }
  }

  @Nullable
  private TextureRegion tileRegionAt(FlixelTilemapLayer layer, int mapCol, int mapRow) {
    int col = mapCol;
    int row = mapRow;
    if (loopX) {
      col = ((col % mapWidth) + mapWidth) % mapWidth;
    } else if (col < 0 || col >= mapWidth) {
      return null;
    }
    if (loopY) {
      row = ((row % mapHeight) + mapHeight) % mapHeight;
    } else if (row < 0 || row >= mapHeight) {
      return null;
    }
    int id = layer.tiles[row * mapWidth + col];
    if (id <= 0) {
      return null;
    }
    return layer.tileset.getRegion(id);
  }

  private void ensureGridSizing(FlixelCamera cam) {
    if (gridCols > 0) {
      return;
    }
    gridCols = (int) Math.ceil(cam.getViewWidth() / tileWidth) + 2;
    gridRows = (int) Math.ceil(cam.getViewHeight() / tileHeight) + 2;
  }

  private int computeOriginCol(FlixelCamera cam, FlixelTilemapLayer layer) {
    float localLeft = cam.scrollX * layer.scrollFactorX + cam.getViewMarginX() - getX();
    return (int) Math.floor(localLeft / tileWidth) - 1;
  }

  private int computeOriginRow(FlixelCamera cam, FlixelTilemapLayer layer) {
    float localTop = cam.scrollY * layer.scrollFactorY + cam.getViewMarginY() - getY();
    return (int) Math.floor(localTop / tileHeight) - 1;
  }

  private int normalizeCol(int col) {
    return loopX ? ((col % mapWidth) + mapWidth) % mapWidth : col;
  }

  private int normalizeRow(int row) {
    return loopY ? ((row % mapHeight) + mapHeight) % mapHeight : row;
  }

  private void placeTileCollider(int col, int row) {
    float tileX = getX() + col * (float) tileWidth;
    float tileY = getY() + row * (float) tileHeight;
    tileCollider.configure(tileX, tileY, tileWidth, tileHeight);
  }

  private boolean objectOverlapsTile(FlixelObject obj, int col, int row) {
    float tileX = getX() + col * (float) tileWidth;
    float tileY = getY() + row * (float) tileHeight;
    return obj.getX() < tileX + tileWidth && obj.getX() + obj.getWidth() > tileX
        && obj.getY() < tileY + tileHeight && obj.getY() + obj.getHeight() > tileY;
  }

  private boolean layersHaveBehaviors() {
    for (int i = 0; i < layers.size; i++) {
      if (layers.get(i).behaviors != null) {
        return true;
      }
    }
    return false;
  }

  private void fireBehaviorContacts(FlixelObject obj, @Nullable IntArray previous) {
    for (int i = 0; i < scratchContacts.size; i++) {
      int key = scratchContacts.get(i);
      int li = key >>> CONTACT_LAYER_SHIFT;
      int col = key & CONTACT_COORD_MASK;
      int row = (key >>> CONTACT_COORD_BITS) & CONTACT_COORD_MASK;
      FlixelTilemapLayer layer = layers.get(li);
      FlixelTileBehavior behavior = layer.getBehavior(getTileAtMapPos(col, row, li));
      if (behavior == null) {
        continue;
      }
      if (previous == null || !previous.contains(key)) {
        behavior.onEnter(this, obj, col, row, layer);
      }
      behavior.onTouch(this, obj, col, row, layer);
    }

    if (previous != null) {
      for (int i = 0; i < previous.size; i++) {
        int key = previous.get(i);
        if (scratchContacts.contains(key)) {
          continue;
        }
        int li = key >>> CONTACT_LAYER_SHIFT;
        int col = key & CONTACT_COORD_MASK;
        int row = (key >>> CONTACT_COORD_BITS) & CONTACT_COORD_MASK;
        FlixelTilemapLayer layer = layers.get(li);
        FlixelTileBehavior behavior = layer.getBehavior(getTileAtMapPos(col, row, li));
        if (behavior != null) {
          behavior.onLeave(this, obj, col, row, layer);
        }
      }
    }

    if (scratchContacts.size == 0) {
      if (contacts != null) {
        IntArray stored = contacts.get(obj);
        if (stored != null) {
          stored.clear();
        }
      }
      return;
    }
    if (contacts == null) {
      contacts = new ObjectMap<>();
    }
    IntArray stored = contacts.get(obj);
    if (stored == null) {
      stored = new IntArray(scratchContacts.size);
      contacts.put(obj, stored);
    }
    stored.clear();
    stored.addAll(scratchContacts);
  }

  @Nullable
  private FlixelCamera resolveCamera() {
    FlixelCamera cam = Flixel.getDrawCamera();
    if (cam == null && Flixel.cameras.size > 0) {
      cam = Flixel.cameras.first();
    }
    return cam;
  }

  private static int packContact(int layerIndex, int col, int row) {
    return (layerIndex << CONTACT_LAYER_SHIFT)
        | ((row & CONTACT_COORD_MASK) << CONTACT_COORD_BITS)
        | (col & CONTACT_COORD_MASK);
  }

  private static boolean containsIdentity(Array<?> array, Object value) {
    for (int i = 0; i < array.size; i++) {
      if (array.get(i) == value) {
        return true;
      }
    }
    return false;
  }

  /** Returns the map width in tiles. */
  public int getMapWidth() {
    return mapWidth;
  }

  /** Returns the map height in tiles. */
  public int getMapHeight() {
    return mapHeight;
  }

  /** Returns the tile width in pixels. */
  public int getTileWidth() {
    return tileWidth;
  }

  /** Returns the tile height in pixels. */
  public int getTileHeight() {
    return tileHeight;
  }

  /** Returns the number of visible grid columns, or {@code 0} before the first draw. */
  public int getGridCols() {
    return gridCols;
  }

  /** Returns the number of visible grid rows, or {@code 0} before the first draw. */
  public int getGridRows() {
    return gridRows;
  }

  /** Returns the number of layers on this map. */
  public int getLayerCount() {
    return layers.size;
  }

  /**
   * Reusable immovable stand-in placed at a solid tile so the framework's object separation logic
   * can push a colliding object out of it without allocating a new object per tile.
   */
  private static final class TileCollider extends FlixelObject {

    TileCollider() {
      super();
      setImmovable(true);
    }

    void configure(float x, float y, float width, float height) {
      setPosition(x, y);
      setSize(width, height);
      lastX = x;
      lastY = y;
      velocityX = 0f;
      velocityY = 0f;
      setImmovable(true);
      setAllowCollisions(FlixelDirectionFlags.ANY);
    }
  }
}
