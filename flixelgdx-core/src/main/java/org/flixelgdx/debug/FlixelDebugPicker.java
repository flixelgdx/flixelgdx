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

import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelBasic;
import org.flixelgdx.FlixelCamera;
import org.flixelgdx.FlixelObject;
import org.flixelgdx.FlixelSprite;
import org.flixelgdx.FlixelState;
import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.debug.panels.FlixelDebugControlsPanel;
import org.flixelgdx.debug.panels.FlixelDebugViewportPanel;
import org.flixelgdx.functional.IFlixelBasic;
import org.flixelgdx.graphics.FlixelFrame;
import org.flixelgdx.group.FlixelGroupable;
import org.flixelgdx.math.FlixelMath;
import org.flixelgdx.math.FlixelVector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Finds the object that is drawn at a point on the screen, the way a player would point at it.
 *
 * <p>Think of looking down at a stack of cards on a table and tapping one. The card on top is the one you
 * touched, even though cards underneath also cover that spot. The picker works the same way: it checks the
 * objects in the reverse of their draw order, so the one drawn last (on top) is found first.
 *
 * <p>For every camera, from the top camera down, the picker converts the screen point into that camera's view
 * space, which accounts for the camera's zoom, then converts each object's position with the camera's scroll and
 * the object's own scroll factor (so parallax layers are hit where they are drawn). Only objects that exist, are
 * visible, and are drawn on that camera count. Sprites are tested against the area they are drawn in (their graphic
 * with its offset and scale, widened to the bounding box when rotated), other objects against their hitbox.
 * With {@link #hitboxes} turned on, every object is tested against its hitbox instead, sprites included.
 * Groups and {@link FlixelDebugNode}s are searched child by child, last child first, and a hidden or removed
 * group hides its members. A group itself covers the area around all of its drawn members, so a point between
 * two members that is not on any of them hits the group.
 *
 * <p>The same walk is shared with anything else that needs "every object drawn on this camera", such as the
 * {@linkplain FlixelDebugViewportPanel viewport panel} that sends a map of the screen to the debugger app. Call
 * {@link #forEachDrawn(FlixelCamera, boolean, Visitor)} to walk the objects in either order, and
 * {@link #computeBounds(FlixelObject, FlixelCamera)} to measure one of them, so every tool agrees on what is drawn
 * where.
 *
 * <p>Nothing here allocates.
 *
 * <p>Example:
 *
 * <pre>{@code
 * FlixelDebugPicker picker = new FlixelDebugPicker();
 * Object hit = picker.pickAt(Flixel.mouse.getScreenX(), Flixel.mouse.getScreenY());
 * if (hit != null) {
 *   Flixel.debug.inspector.select(hit);
 * }
 * }</pre>
 *
 * @see FlixelDebugControlsPanel
 * @see FlixelDebugViewportPanel
 */
public class FlixelDebugPicker {

  private float boundsLeft;
  private float boundsTop;
  private float boundsWidth;
  private float boundsHeight;
  private float hitX;
  private float hitY;

  private final float[] nodeBounds = new float[4];
  private final FlixelVector point = new FlixelVector();
  private final Visitor hitTest = this::testHit;

  /**
   * Sets whether objects are measured and hit by their hitbox instead of the area they are drawn in.
   *
   * <p>A hitbox is the collision box ({@link FlixelObject#getWidth()} and {@link FlixelObject#getHeight()} at the
   * object's position). It does not follow a sprite's offset, scale, angle, or animation frame, so it only changes
   * when the game moves the object or resizes its hitbox. The {@linkplain FlixelDebugViewportPanel viewport panel}
   * turns this on so the debugger app shows and picks the same boxes the game collides with.
   */
  public boolean hitboxes;

  @Nullable
  private Object hit;

  /** Creates a picker. */
  public FlixelDebugPicker() {}

  /**
   * Finds the topmost object at a point in window coordinates.
   *
   * @param screenX Pixels from the left edge of the window.
   * @param screenY Pixels from the top edge of the window.
   * @return The object, or {@code null} when nothing is drawn there.
   */
  @Nullable
  public Object pickAt(float screenX, float screenY) {
    FlixelArray<FlixelCamera> cameras = Flixel.cameras;
    for (int i = cameras.getSize() - 1; i >= 0; i--) {
      FlixelCamera cam = cameras.get(i);
      if (cam == null || !cam.exists) {
        continue;
      }
      point.set(screenX, screenY);
      cam.unprojectToView(point);
      Object found = pickInCamera(cam, point.x, point.y);
      if (found != null) {
        return found;
      }
    }
    return null;
  }

  /**
   * Finds the topmost object at a point that is already in a camera's view space.
   *
   * <p>View space is the space objects are drawn in before the projection: {@code 0, 0} is the top left corner of
   * what the camera shows, and one unit is one world unit at the camera's zoom.
   *
   * @param cam The camera the point belongs to.
   * @param viewX The horizontal position in view space.
   * @param viewY The vertical position in view space.
   * @return The object, or {@code null} when the point is outside the camera's view or nothing is drawn there.
   */
  @Nullable
  public Object pickInCamera(@NotNull FlixelCamera cam, float viewX, float viewY) {
    float zoom = cam.getZoom();
    if (viewX < 0f || viewY < 0f || viewX > cam.getWorldWidth() / zoom || viewY > cam.getWorldHeight() / zoom) {
      return null;
    }
    hitX = viewX;
    hitY = viewY;
    forEachDrawn(cam, true, hitTest);
    Object found = hit;
    hit = null;
    return found;
  }

  /**
   * Visits every object that is drawn on a camera, in draw order or in the reverse of it.
   *
   * <p>This is the walk the picker itself uses, so a tool built on it sees exactly the objects a click can hit.
   * It starts at {@link Flixel#state} and follows the open sub states. A state below an open sub state only counts
   * while it still draws behind it ({@link FlixelState#persistentDraw}). Members that do not exist, are not
   * visible, or are not drawn on the camera are skipped, and a group that is skipped hides all of its members.
   * Groups are visited too (they are members like any other), and a group's members are drawn after the group, so
   * they count as being on top of it. The children of a {@link FlixelDebugNode} are walked like group members. Objects nested deeper than {@link FlixelDebugPaths#MAX_DEPTH} are not
   * visited.
   *
   * <p>It does not allocate. The visitor may call {@link #computeBounds(FlixelObject, FlixelCamera)} on this
   * picker, but it must not change the scene while the walk runs.
   *
   * <p>Example:
   *
   * <pre>{@code
   * // List every sprite drawn on the main camera, bottom to top. Make the visitor once, not every frame.
   * FlixelDebugPicker.Visitor printer = (member, cam) -> {
   *   if (member instanceof FlixelSprite sprite) {
   *     Flixel.info("Sprite at {}, {}", sprite.getX(), sprite.getY());
   *   }
   *   return false; // Keep going.
   * };
   * picker.forEachDrawn(Flixel.cameras.first(), false, printer);
   * }</pre>
   *
   * @param cam The camera whose objects are visited.
   * @param topFirst {@code true} to visit the object drawn last (on top) first, the way a click searches.
   *     {@code false} visits in draw order, bottom first.
   * @param visitor Called once per drawn member. It returns {@code true} to stop the walk.
   * @return {@code true} when the visitor stopped the walk, or {@code false} when every object was visited.
   */
  public boolean forEachDrawn(@NotNull FlixelCamera cam, boolean topFirst, @NotNull Visitor visitor) {
    FlixelState state = Flixel.state;
    return state != null && walkLevel(state, cam, topFirst, visitor, 0);
  }

  /**
   * Works out the area an object is drawn in on a camera, in the camera's view space.
   *
   * <p>A sprite with a graphic is measured the way {@link FlixelSprite#draw} places it: from its position plus
   * its offset, as wide and tall as its graphic frame times its scale, and widened to the bounding box when it is
   * rotated. Any other object, and a sprite without a graphic, uses its hitbox. While {@link #hitboxes} is
   * {@code true}, every object uses its hitbox, placed with its scroll factor. The result is stored in this picker
   * and read with {@link #getBoundsLeft()}, {@link #getBoundsTop()}, {@link #getBoundsWidth()}, and
   * {@link #getBoundsHeight()}.
   *
   * @param object The object to measure.
   * @param cam The camera it is drawn on.
   * @return {@code true} when the area has a size. When {@code false}, the stored values are not meaningful.
   */
  public boolean computeBounds(@NotNull FlixelObject object, @NotNull FlixelCamera cam) {
    float left = cam.worldToViewX(object.getX(), object.getScrollX());
    float top = cam.worldToViewY(object.getY(), object.getScrollY());
    float width = object.getWidth();
    float height = object.getHeight();
    if (!hitboxes && object instanceof FlixelSprite sprite) {
      FlixelFrame frame = sprite.getFrame();
      if (frame != null) {
        // Mirror FlixelSprite.draw(): the whole source box is scaled around its center and then anchored so its
        // top left corner stays at the sprite's drawn position (position minus offset).
        left -= sprite.getOffsetX();
        top -= sprite.getOffsetY();
        width = frame.originalWidth * Math.abs(sprite.getScaleX());
        height = frame.originalHeight * Math.abs(sprite.getScaleY());
        float angle = sprite.getAngle();
        if (angle != 0f) {
          float cos = Math.abs(FlixelMath.cosDeg(angle));
          float sin = Math.abs(FlixelMath.sinDeg(angle));
          float rotW = cos * width + sin * height;
          float rotH = sin * width + cos * height;
          left -= (rotW - width) * 0.5f;
          top -= (rotH - height) * 0.5f;
          width = rotW;
          height = rotH;
        }
      }
    }
    boundsLeft = left;
    boundsTop = top;
    boundsWidth = width;
    boundsHeight = height;
    return width > 0f && height > 0f;
  }

  /**
   * Works out the area any walkable object covers on a camera, in the camera's view space.
   *
   * <p>A {@link FlixelDebugNode} measures itself through {@link FlixelDebugNode#getDebugBounds(FlixelCamera,
   * float[])}. A {@link FlixelGroupable} covers the smallest box around the areas of its members that are drawn on
   * the camera (the same members {@link #forEachDrawn(FlixelCamera, boolean, Visitor)} visits), so a group is
   * outlined around what it holds even when it has no size of its own. A group with nothing drawn falls back to
   * its own hitbox when it is a {@link FlixelObject}. Any other {@link FlixelObject} is measured by
   * {@link #computeBounds(FlixelObject, FlixelCamera)}. Anything else has no area. The result is read with {@link #getBoundsLeft()}, {@link #getBoundsTop()},
   * {@link #getBoundsWidth()}, and {@link #getBoundsHeight()}. This does not allocate.
   *
   * @param node The object to measure.
   * @param cam The camera it is drawn on.
   * @return {@code true} when the object has an area on the camera. When {@code false}, the stored values are not
   *     meaningful.
   */
  public boolean measure(@NotNull Object node, @NotNull FlixelCamera cam) {
    return measure(node, cam, 0);
  }

  private boolean measure(Object node, FlixelCamera cam, int depth) {
    if (node instanceof FlixelDebugNode debug) {
      if (!debug.getDebugBounds(cam, nodeBounds)) {
        return false;
      }
      boundsLeft = nodeBounds[0];
      boundsTop = nodeBounds[1];
      boundsWidth = nodeBounds[2];
      boundsHeight = nodeBounds[3];
      return boundsWidth > 0f && boundsHeight > 0f;
    }
    if (node instanceof FlixelGroupable<?> && measureMembers(node, cam, depth)) {
      return true;
    }
    return node instanceof FlixelObject object && computeBounds(object, cam);
  }

  /** Measures a group as the smallest box around its drawn members, nested groups included. */
  private boolean measureMembers(Object group, FlixelCamera cam, int depth) {
    if (depth >= FlixelDebugPaths.MAX_DEPTH) {
      return false;
    }
    float left = Float.POSITIVE_INFINITY;
    float top = Float.POSITIVE_INFINITY;
    float right = Float.NEGATIVE_INFINITY;
    float bottom = Float.NEGATIVE_INFINITY;
    int size = FlixelDebugNodes.childCount(group);
    for (int k = 0; k < size; k++) {
      Object member = FlixelDebugNodes.child(group, k);
      // Measuring a member overwrites the stored bounds, so the running box is kept in locals.
      if (!isShown(member, cam) || !measure(member, cam, depth + 1)) {
        continue;
      }
      left = Math.min(left, boundsLeft);
      top = Math.min(top, boundsTop);
      right = Math.max(right, boundsLeft + boundsWidth);
      bottom = Math.max(bottom, boundsTop + boundsHeight);
    }
    if (left >= right || top >= bottom) {
      return false;
    }
    boundsLeft = left;
    boundsTop = top;
    boundsWidth = right - left;
    boundsHeight = bottom - top;
    return true;
  }

  /** Returns whether a member is drawn on a camera: it exists, is visible, and draws on that camera. */
  private static boolean isShown(@Nullable Object member, FlixelCamera cam) {
    return switch (member) {
      case null -> false;
      case IFlixelBasic basic when (!basic.isExists() || !basic.isVisible()) -> false;
      case FlixelBasic basic when !isDrawnOn(basic, cam) -> false;
      default -> true;
    };
  }

  /**
   * Returns whether an object is drawn on a camera.
   *
   * @param object The object, whose {@link FlixelBasic#cameras} list is checked.
   * @param cam The camera.
   * @return {@code true} when the list is empty and the camera is a default draw target, or the list holds the
   *     camera.
   */
  public static boolean isDrawnOn(@NotNull FlixelBasic object, @NotNull FlixelCamera cam) {
    FlixelCamera[] list = object.cameras;
    if (list == null || list.length == 0) {
      return cam.defaultDrawTarget;
    }
    for (int i = 0; i < list.length; i++) {
      if (list[i] == cam) {
        return true;
      }
    }
    return false;
  }

  private boolean walkLevel(FlixelState level, FlixelCamera cam, boolean topFirst, Visitor visitor, int levels) {
    FlixelState sub = level.getSubState();
    boolean top = sub == null || levels + 1 >= FlixelDebugPaths.MAX_DEPTH;
    // The open substate is drawn last, so it is on top. A state below it only counts while it still draws
    // behind the substate.
    boolean drawn = top || level.persistentDraw;
    if (topFirst) {
      return (!top && walkLevel(sub, cam, true, visitor, levels + 1))
          || (drawn && walkGroup(level, cam, true, visitor, 0));
    }
    return (drawn && walkGroup(level, cam, false, visitor, 0))
        || (!top && walkLevel(sub, cam, false, visitor, levels + 1));
  }

  private boolean walkGroup(Object node, FlixelCamera cam, boolean topFirst, Visitor visitor, int depth) {
    if (depth >= FlixelDebugPaths.MAX_DEPTH) {
      return false;
    }
    int size = FlixelDebugNodes.childCount(node);
    for (int k = 0; k < size; k++) {
      Object member = FlixelDebugNodes.child(node, topFirst ? size - 1 - k : k);
      if (!isShown(member, cam)) {
        continue;
      }
      // Children are drawn after their parent, so they are on top of it.
      if (topFirst) {
        if (walkGroup(member, cam, true, visitor, depth + 1) || visitor.visit(member, cam)) {
          return true;
        }
      } else if (visitor.visit(member, cam) || walkGroup(member, cam, false, visitor, depth + 1)) {
        return true;
      }
    }
    return false;
  }

  private boolean testHit(Object member, FlixelCamera cam) {
    if (measure(member, cam)
        && hitX >= boundsLeft && hitX <= boundsLeft + boundsWidth
        && hitY >= boundsTop && hitY <= boundsTop + boundsHeight) {
      hit = member;
      return true;
    }
    return false;
  }

  public float getBoundsLeft() {
    return boundsLeft;
  }

  public float getBoundsTop() {
    return boundsTop;
  }

  public float getBoundsWidth() {
    return boundsWidth;
  }

  public float getBoundsHeight() {
    return boundsHeight;
  }

  /**
   * Receives the objects of {@link #forEachDrawn(FlixelCamera, boolean, Visitor)} one at a time.
   *
   * <p>Keep one instance in a field (for example a method reference made once) so walking every frame does not
   * allocate.
   */
  public interface Visitor {

    /**
     * Looks at one drawn object.
     *
     * @param member The object. It exists, is visible, and is drawn on {@code cam}. It may be a group.
     * @param cam The camera being walked.
     * @return {@code true} to stop the walk here, or {@code false} to keep going.
     */
    boolean visit(@NotNull Object member, @NotNull FlixelCamera cam);
  }
}
