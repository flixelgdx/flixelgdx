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

import org.flixelgdx.FlixelObject;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * One layer of tile data inside a {@link FlixelTilemap}, plus the recycled-grid (ring buffer) state
 * used to draw it.
 *
 * <p>Every layer owns its own flat, row-major tile array, its own tileset, and its own set of
 * visible tile slots, so layers can scroll at different parallax rates and use different art. The
 * tile array stores 1-based tile IDs where {@code 0} means "empty" (nothing drawn). A layer can
 * optionally mark certain tile IDs as solid (for collision) and attach {@link FlixelTileBehavior}s
 * to certain tile IDs (for gameplay logic).
 *
 * <p>Layers are created through {@link FlixelTilemap#addLayer(String, int[], FlixelTileset)}; you
 * do not construct them directly in normal use. The ring buffer fields are managed by the owning
 * tilemap and are package-private on purpose.
 *
 * @see FlixelTilemap
 * @see FlixelTileset
 * @see FlixelTileBehavior
 */
public final class FlixelTilemapLayer {

  /** Last camera scroll X the ring buffer was synced to. {@link Float#NaN} until the first sync. */
  float lastScrollX = Float.NaN;

  /** Last camera scroll Y the ring buffer was synced to. {@link Float#NaN} until the first sync. */
  float lastScrollY = Float.NaN;

  /** Horizontal parallax factor. {@code 1} moves with the camera, {@code 0.5} moves at half speed. */
  float scrollFactorX = 1f;

  /** Vertical parallax factor. {@code 1} moves with the camera, {@code 0.5} moves at half speed. */
  float scrollFactorY = 1f;

  /** Physical grid column index that currently holds visible column 0 (the ring buffer head). */
  int ringOriginX;

  /** Physical grid row index that currently holds visible row 0 (the ring buffer head). */
  int ringOriginY;

  /** Map column shown in visible column 0. May be negative or beyond the map when looping. */
  int mapOriginCol;

  /** Map row shown in visible row 0. May be negative or beyond the map when looping. */
  int mapOriginRow;

  /** Flat, row-major tile IDs. Index {@code row * mapWidth + col}. 1-based IDs, {@code 0} = empty. */
  int[] tiles;

  /** The tileset this layer draws its tiles from. */
  FlixelTileset tileset;

  /**
   * Multiply tint applied to every tile on this layer when drawn. Defaults to opaque white, which
   * leaves the art untouched. Lower the red, green, and blue channels together to darken the layer
   * (a cheap depth cue for background layers), or lower the alpha to fade the whole layer out.
   */
  final Color tint = new Color(Color.WHITE);

  /** Recycled visible slots, indexed {@code [physicalCol][physicalRow]}. Lazily allocated. */
  @Nullable
  TextureRegion[][] grid;

  /** Behaviors indexed by tile ID; {@code null} entry means no behavior. Lazily allocated. */
  @Nullable
  FlixelTileBehavior[] behaviors;

  /** Solid flags indexed by tile ID; {@code true} means the tile blocks movement. Lazily allocated. */
  @Nullable
  boolean[] solidTiles;

  /** Human-readable layer name, used for lookups by name. */
  String name;

  /** Whether this layer is drawn. Layers still collide and update behaviors while hidden. */
  boolean visible = true;

  /**
   * Creates a layer. Called by {@link FlixelTilemap#addLayer(String, int[], FlixelTileset)}.
   *
   * @param name The layer's name.
   * @param tiles The flat, row-major tile IDs. Not copied; the layer uses this array directly.
   * @param tileset The tileset used to draw the layer's tiles.
   */
  FlixelTilemapLayer(String name, int[] tiles, FlixelTileset tileset) {
    this.name = name;
    this.tiles = tiles;
    this.tileset = tileset;
  }

  /**
   * Marks one or more tile IDs as solid so a {@link FlixelObject} passed to
   * {@link FlixelTilemap#collide(FlixelObject)} is pushed out of them.
   *
   * @param tileIds The 1-based tile IDs to mark solid.
   */
  public void setSolid(int... tileIds) {
    if (tileIds == null || tileIds.length == 0) {
      return;
    }
    int max = 0;
    for (int id : tileIds) {
      if (id > max) {
        max = id;
      }
    }
    ensureSolidCapacity(max);
    for (int id : tileIds) {
      if (id >= 1) {
        solidTiles[id] = true;
      }
    }
  }

  /** Clears every solid flag on this layer, so no tile blocks movement. */
  public void clearSolid() {
    if (solidTiles != null) {
      Arrays.fill(solidTiles, false);
    }
  }

  /**
   * Returns whether the tile at the given map position is solid. Out-of-range positions are treated
   * as not solid.
   *
   * @param mapCol The tile column.
   * @param mapRow The tile row.
   * @param mapWidth The map width in tiles.
   * @param mapHeight The map height in tiles.
   * @return {@code true} if a solid tile occupies that position.
   */
  public boolean isSolidAt(int mapCol, int mapRow, int mapWidth, int mapHeight) {
    if (solidTiles == null || mapCol < 0 || mapRow < 0 || mapCol >= mapWidth || mapRow >= mapHeight) {
      return false;
    }
    int id = tiles[mapRow * mapWidth + mapCol];
    return id >= 1 && id < solidTiles.length && solidTiles[id];
  }

  /**
   * Registers a behavior for a tile ID. Pass {@code null} to remove a previously registered
   * behavior.
   *
   * @param tileId The 1-based tile ID.
   * @param behavior The behavior to run for tiles of that ID, or {@code null} to clear it.
   */
  public void setBehavior(int tileId, @Nullable FlixelTileBehavior behavior) {
    if (tileId < 1) {
      return;
    }
    if (behavior == null) {
      if (behaviors != null && tileId < behaviors.length) {
        behaviors[tileId] = null;
      }
      return;
    }
    ensureBehaviorCapacity(tileId);
    behaviors[tileId] = behavior;
  }

  /**
   * Returns the behavior registered for the given tile ID, or {@code null} if none.
   *
   * @param tileId The 1-based tile ID.
   * @return The registered behavior, or {@code null}.
   */
  @Nullable
  public FlixelTileBehavior getBehavior(int tileId) {
    if (behaviors == null || tileId < 0 || tileId >= behaviors.length) {
      return null;
    }
    return behaviors[tileId];
  }

  /**
   * Sets this layer's parallax scroll factors. A factor of {@code 1} scrolls the layer in lockstep
   * with the camera; smaller values make it drift more slowly for a depth effect.
   *
   * @param scrollFactorX Horizontal parallax factor.
   * @param scrollFactorY Vertical parallax factor.
   */
  public void setScrollFactor(float scrollFactorX, float scrollFactorY) {
    this.scrollFactorX = scrollFactorX;
    this.scrollFactorY = scrollFactorY;
  }

  /**
   * Darkens or brightens this layer uniformly by setting its tint's red, green, and blue channels to
   * the same value. {@code 1} leaves the art at full brightness, {@code 0.5} makes it half as
   * bright, and {@code 0} turns it black. The alpha channel is left unchanged.
   *
   * @param brightness The brightness multiplier, typically in the range {@code 0} to {@code 1}.
   */
  public void setBrightness(float brightness) {
    tint.set(brightness, brightness, brightness, tint.a);
  }

  /**
   * Sets this layer's multiply tint. Copies the given color's channels, so later changes to
   * {@code color} do not affect the layer.
   *
   * @param color The tint to apply. Must not be {@code null}.
   */
  public void setTint(Color color) {
    tint.set(color);
  }

  /**
   * Sets this layer's multiply tint from raw channel values in the range {@code 0} to {@code 1}.
   *
   * @param r The red channel.
   * @param g The green channel.
   * @param b The blue channel.
   * @param a The alpha channel; lower it to fade the whole layer out.
   */
  public void setTint(float r, float g, float b, float a) {
    tint.set(r, g, b, a);
  }

  private void ensureSolidCapacity(int maxTileId) {
    int needed = maxTileId + 1;
    if (solidTiles == null) {
      solidTiles = new boolean[needed];
    } else if (solidTiles.length < needed) {
      boolean[] grown = new boolean[needed];
      System.arraycopy(solidTiles, 0, grown, 0, solidTiles.length);
      solidTiles = grown;
    }
  }

  private void ensureBehaviorCapacity(int maxTileId) {
    int needed = maxTileId + 1;
    if (behaviors == null) {
      behaviors = new FlixelTileBehavior[needed];
    } else if (behaviors.length < needed) {
      FlixelTileBehavior[] grown = new FlixelTileBehavior[needed];
      System.arraycopy(behaviors, 0, grown, 0, behaviors.length);
      behaviors = grown;
    }
  }

  /** Returns this layer's tileset. */
  public FlixelTileset getTileset() {
    return tileset;
  }

  /** Returns the horizontal parallax factor. */
  public float getScrollFactorX() {
    return scrollFactorX;
  }

  /** Returns the vertical parallax factor. */
  public float getScrollFactorY() {
    return scrollFactorY;
  }

  /**
   * Returns this layer's live multiply tint. Mutating the returned {@link Color} changes the layer's
   * tint directly.
   */
  public Color getTint() {
    return tint;
  }

  /** Returns this layer's name. */
  public String getName() {
    return name;
  }

  /** Returns whether this layer is drawn. */
  public boolean isVisible() {
    return visible;
  }

  /** Returns whether this layer is drawn. */
  public boolean getVisible() {
    return visible;
  }

  /**
   * Sets whether this layer is drawn. A hidden layer still collides and still updates its
   * behaviors.
   *
   * @param visible {@code true} to draw the layer.
   */
  public void setVisible(boolean visible) {
    this.visible = visible;
  }
}
