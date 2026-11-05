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
 * Groups are searched member by member, last member first, and a hidden or removed group hides its members.
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
 */
public class FlixelDebugPicker {

  private float boundsLeft;
  private float boundsTop;
  private float boundsWidth;
  private float boundsHeight;

  private final FlixelVector point = new FlixelVector();
  private final FlixelState[] chain = new FlixelState[FlixelDebugPaths.MAX_DEPTH];

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
      cam.unproject(point);
      Object hit = pickInCamera(cam, point.x, point.y);
      if (hit != null) {
        return hit;
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
    int levels = 0;
    for (FlixelState level = Flixel.state; level != null && levels < chain.length; level = level.getSubState()) {
      chain[levels++] = level;
    }
    // The open sub state is drawn last, so it is on top and is checked first. A state below it only counts
    // while it still draws behind the sub state.
    Object hit = null;
    for (int i = levels - 1; i >= 0 && hit == null; i--) {
      FlixelState level = chain[i];
      if (i == levels - 1 || level.persistentDraw) {
        hit = pickInGroup(level, cam, viewX, viewY, 0);
      }
    }
    for (int i = 0; i < levels; i++) {
      chain[i] = null;
    }
    return hit;
  }

  /**
   * Works out the area an object is drawn in on a camera, in the camera's view space.
   *
   * <p>A sprite with a graphic is measured the way {@link FlixelSprite#draw} places it: from its position plus
   * its offset, as wide and tall as its graphic frame times its scale, and widened to the bounding box when it is
   * rotated. Any other object, and a sprite without a graphic, uses its hitbox. The result is stored in this picker
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
    if (object instanceof FlixelSprite sprite) {
      FlixelFrame frame = sprite.getCurrentFrame();
      if (frame != null) {
        // Mirror FlixelSprite.draw(): the whole source box is scaled around its center and then anchored so its
        // top left corner stays at the sprite's drawn position (position plus offset).
        left += sprite.getOffsetX();
        top += sprite.getOffsetY();
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

  @Nullable
  private Object pickInGroup(Object node, FlixelCamera cam, float viewX, float viewY, int depth) {
    if (depth >= FlixelDebugPaths.MAX_DEPTH || !(node instanceof FlixelGroupable<?> group)) {
      return null;
    }
    FlixelArray<?> members = group.getMembers();
    if (members == null) {
      return null;
    }
    for (int i = members.getSize() - 1; i >= 0; i--) {
      Object member = members.get(i);
      if (member == null) {
        continue;
      }
      if (member instanceof IFlixelBasic basic && (!basic.isExists() || !basic.isVisible())) {
        continue;
      }
      if (member instanceof FlixelBasic basic && !isDrawnOn(basic, cam)) {
        continue;
      }
      // Children are drawn after their parent, so they are on top of it.
      Object inside = pickInGroup(member, cam, viewX, viewY, depth + 1);
      if (inside != null) {
        return inside;
      }
      if (member instanceof FlixelObject object && contains(object, cam, viewX, viewY)) {
        return object;
      }
    }
    return null;
  }

  private boolean contains(FlixelObject object, FlixelCamera cam, float viewX, float viewY) {
    return computeBounds(object, cam)
        && viewX >= boundsLeft && viewX <= boundsLeft + boundsWidth
        && viewY >= boundsTop && viewY <= boundsTop + boundsHeight;
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
}
