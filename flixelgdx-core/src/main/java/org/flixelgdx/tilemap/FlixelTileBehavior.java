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

import org.flixelgdx.FlixelObject;
import org.flixelgdx.functional.FlixelDestroyable;
import org.flixelgdx.functional.FlixelUpdatable;

/**
 * Attaches custom logic to a tile <em>type</em> (a tile ID) inside a {@link FlixelTilemap}.
 *
 * <p>A behavior is a flyweight: you create one instance per tile type and register it once on a
 * {@link FlixelTilemapLayer} with {@link FlixelTilemapLayer#setBehavior(int, FlixelTileBehavior)}.
 * That single instance then handles every tile of that type across the whole map. Because the map
 * can be enormous while only a handful of tile types exist, this keeps memory flat: there is no
 * per-tile object, only per-type. Each callback receives the specific tile coordinates that
 * triggered it, so the shared instance always knows which tile it is acting on.
 *
 * <p>All callbacks are no-ops by default, so subclasses only override the ones they care about.
 * The class implements {@link FlixelUpdatable} and {@link FlixelDestroyable} so it slots into the
 * same lifecycle model as the rest of the framework.
 *
 * <p><b>Example - a block that breaks when the player interacts with it:</b>
 *
 * <pre>{@code
 * class BreakableTile extends FlixelTileBehavior {
 *   @Override
 *   public void onInteract(FlixelTilemap map, int col, int row, FlixelTilemapLayer layer) {
 *     // Replace the tile with 0 (empty), removing it from the map.
 *     map.setTileAtMapPos(col, row, map.getLayerIndex(layer), 0);
 *   }
 * }
 *
 * layer.setBehavior(3, new BreakableTile()); // Every tile with ID 3 now breaks on interact.
 * }</pre>
 *
 * @see FlixelTilemap
 * @see FlixelTilemapLayer
 */
public abstract class FlixelTileBehavior implements FlixelUpdatable, FlixelDestroyable {

  /**
   * Advances any time-based state this behavior keeps (timers, animation counters, state machines).
   *
   * <p>Called once per frame by {@link FlixelTilemap#update(float)} for each tile ID this behavior
   * is registered to. If you register the same instance for more than one tile ID, this runs once
   * per registration each frame.
   *
   * @param elapsed Seconds elapsed since the last frame.
   */
  @Override
  public void update(float elapsed) {}

  /**
   * Releases any resources this behavior holds. Called when the owning {@link FlixelTilemap} is
   * destroyed. The default implementation does nothing.
   */
  @Override
  public void destroy() {}

  /**
   * Called on the first frame a {@link FlixelObject} begins overlapping a tile of this type at the
   * given position, detected during {@link FlixelTilemap#collide(FlixelObject)}.
   *
   * @param map The tilemap that owns the tile.
   * @param obj The object that started touching the tile.
   * @param col The tile's map column.
   * @param row The tile's map row.
   * @param layer The layer the tile belongs to.
   */
  public void onEnter(FlixelTilemap map, FlixelObject obj, int col, int row, FlixelTilemapLayer layer) {}

  /**
   * Called every frame a {@link FlixelObject} continues overlapping a tile of this type at the given
   * position, detected during {@link FlixelTilemap#collide(FlixelObject)}.
   *
   * @param map The tilemap that owns the tile.
   * @param obj The object touching the tile.
   * @param col The tile's map column.
   * @param row The tile's map row.
   * @param layer The layer the tile belongs to.
   */
  public void onTouch(FlixelTilemap map, FlixelObject obj, int col, int row, FlixelTilemapLayer layer) {}

  /**
   * Called on the first frame after a {@link FlixelObject} stops overlapping a tile of this type at
   * the given position, detected during {@link FlixelTilemap#collide(FlixelObject)}.
   *
   * @param map The tilemap that owns the tile.
   * @param obj The object that stopped touching the tile.
   * @param col The tile's map column.
   * @param row The tile's map row.
   * @param layer The layer the tile belongs to.
   */
  public void onLeave(FlixelTilemap map, FlixelObject obj, int col, int row, FlixelTilemapLayer layer) {}

  /**
   * Called when game code explicitly interacts with a tile of this type through
   * {@link FlixelTilemap#interact(float, float)}. Unlike the collision callbacks, this only fires
   * when you invoke it yourself (for example on a mouse click or a button press), so it is the
   * natural place to put "player activated this tile" logic.
   *
   * @param map The tilemap that owns the tile.
   * @param col The tile's map column.
   * @param row The tile's map row.
   * @param layer The layer the tile belongs to.
   */
  public void onInteract(FlixelTilemap map, int col, int row, FlixelTilemapLayer layer) {}
}
