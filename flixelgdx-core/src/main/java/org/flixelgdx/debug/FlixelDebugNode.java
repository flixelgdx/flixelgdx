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
package org.flixelgdx.debug;

import org.flixelgdx.FlixelCamera;
import org.flixelgdx.FlixelObject;
import org.flixelgdx.debug.feeds.FlixelDebugScene;
import org.flixelgdx.debug.feeds.FlixelDebugViewport;
import org.flixelgdx.functional.FlixelPositional;
import org.flixelgdx.group.FlixelGroupable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Lets an object that is neither a {@link FlixelGroupable} nor a {@link FlixelObject} show up in the debugger.
 *
 * <p>Think of the debugger as a building inspector with a floor plan. Groups are rooms the inspector already
 * knows how to open, and objects are furniture with a size. A user interface widget is neither: it has children
 * and a place on the screen, but the inspector has no key to its door. This interface is the key. It says "these
 * are my children" and "this is the box I cover on that camera", and the debugger does the rest.
 *
 * <p>What a node gets once it implements this interface:
 *
 * <ul>
 *   <li>The {@linkplain FlixelDebugScene scene panel} lists it and descends into its children the way it
 *       does for a group. A child's label is its index, so a scene path such as {@code state.3.0} and
 *       {@code SELECT_PATH} work through a node like through a group.</li>
 *   <li>It gets an object id like every other object, so the inspector can select it.</li>
 *   <li>The {@linkplain FlixelDebugViewport viewport panel} draws its box, a tap picks it, and Alt click
 *       selects it, when {@link #getDebugBounds(FlixelCamera, float[])} returns {@code true}.</li>
 *   <li>The remote {@code CLICK_OBJECT} action clicks the center of that box.</li>
 *   <li>It can be moved from the viewport when it also implements
 *       {@link FlixelPositional}.</li>
 * </ul>
 *
 * <p>When an object implements this interface, it wins over {@link FlixelGroupable} for its children and over
 * {@link FlixelObject} for its bounds. Implement it only on types that the debugger cannot already walk, or when
 * you deliberately want to present a different tree to the debugger.
 *
 * <p>The debugger calls these methods every time it walks the scene, which can be many times per second, so
 * implementations must be cheap and must not allocate. The bounds go into a float array that the caller owns.
 *
 * <p>Example:
 *
 * <pre>{@code
 * public class Panel extends Widget implements FlixelDebugNode {
 *
 *   private final FlixelArray<Widget> children = new FlixelArray<>();
 *
 *   @Override
 *   public int getDebugChildCount() {
 *     return children.getSize();
 *   }
 *
 *   @Override
 *   public Object getDebugChild(int index) {
 *     return children.get(index);
 *   }
 *
 *   @Override
 *   public boolean getDebugBounds(FlixelCamera cam, float[] out) {
 *     if (!isOnCamera(cam)) {
 *       return false;
 *     }
 *     out[0] = cam.worldToViewX(getX(), getScrollX());
 *     out[1] = cam.worldToViewY(getY(), getScrollY());
 *     out[2] = getWidth();
 *     out[3] = getHeight();
 *     return true;
 *   }
 * }
 * }</pre>
 *
 * @see FlixelDebugNodes
 * @see FlixelDebugPaths
 */
public interface FlixelDebugNode {

  /**
   * Returns how many children this node has.
   *
   * <p>Empty slots count, so a child keeps its index even when a neighbor is {@code null}. The index is the step
   * the child takes in a scene path.
   *
   * @return The child count, zero for a leaf (the default).
   */
  default int getDebugChildCount() {
    return 0;
  }

  /**
   * Returns one child.
   *
   * @param index A position from 0 to {@link #getDebugChildCount()} minus 1.
   * @return The child, or {@code null} for an empty slot (the default for a leaf).
   */
  @Nullable
  default Object getDebugChild(int index) {
    return null;
  }

  /**
   * Measures the box this node covers on a camera, in that camera's view space.
   *
   * <p>View space is the space objects are drawn in before the projection: {@code 0, 0} is the top left corner of
   * what the camera shows and one unit is one world unit at the camera's zoom. A node that is placed with
   * {@link FlixelCamera#worldToViewX(float, float)} and {@link FlixelCamera#worldToViewY(float, float)} is already
   * in the right space. The box should be what a person would call "the node", for example a button's whole
   * rectangle, so a click on it hits the node.
   *
   * @param cam The camera the box is measured on.
   * @param out Receives {@code left, top, width, height} in indices 0 to 3. It always has at least four slots.
   * @return {@code true} when the node covers an area on {@code cam}. Return {@code false} when it is not drawn on
   *     that camera, is hidden, or has no area (the default); its children are still walked.
   */
  default boolean getDebugBounds(@NotNull FlixelCamera cam, float[] out) {
    return false;
  }
}
