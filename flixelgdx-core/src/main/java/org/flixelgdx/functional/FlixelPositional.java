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
package org.flixelgdx.functional;

/**
 * Spatial surface shared by {@link org.flixelgdx.FlixelObject FlixelObject}: world position, hitbox size, and
 * scroll factors. Code that only needs to read or set where something sits in the world (camera follow,
 * mouse overlap, motion tweens that only care about placement) should accept this type.
 *
 * <p>For the full kinematic contract (velocity, acceleration, drag, immovable, and so on), see
 * {@link FlixelPhysical}, which extends this interface.
 *
 * @see FlixelPhysical
 * @see org.flixelgdx.FlixelObject
 */
public interface FlixelPositional extends FlixelAngleable {

  /**
   * Parallax scroll factor on X ({@code 1} means the object moves with the camera like a normal world object).
   *
   * @return Current scroll X factor.
   */
  float getScrollX();

  /**
   * Parallax scroll factor on Y ({@code 1} means the object moves with the camera like a normal world object).
   *
   * @return Current scroll Y factor.
   */
  float getScrollY();

  /**
   * Sets {@link #getScrollX()} and {@link #getScrollY()}.
   *
   * @param scrollX Horizontal scroll factor.
   * @param scrollY Vertical scroll factor.
   */
  void setScrollFactor(float scrollX, float scrollY);

  /**
   * X position of the upper left corner of this object in world space.
   *
   * @return Current X in world units.
   */
  float getX();

  /**
   * Sets {@linkplain #getX() world X}.
   *
   * @param x New X position.
   */
  void setX(float x);

  /**
   * Y position of the upper left corner of this object in world space.
   *
   * @return Current Y in world units.
   */
  float getY();

  /**
   * Sets {@linkplain #getY() world Y}.
   *
   * @param y New Y position.
   */
  void setY(float y);

  /**
   * Width of this object's hitbox.
   *
   * @return Hitbox width.
   */
  float getWidth();

  /**
   * Sets {@linkplain #getWidth() hitbox width}.
   *
   * @param width New width.
   */
  void setWidth(float width);

  /**
   * Height of this object's hitbox.
   *
   * @return Hitbox height.
   */
  float getHeight();

  /**
   * Sets {@linkplain #getHeight() hitbox height}.
   *
   * @param height New height.
   */
  void setHeight(float height);

  /**
   * X position at the start of the current frame, before motion.
   *
   * @return Last frame's starting X.
   */
  float getLastX();

  /**
   * Y position at the start of the current frame, before motion.
   *
   * @return Last frame's starting Y.
   */
  float getLastY();

  /**
   * Helper that sets both world coordinates.
   *
   * @param x The new X position.
   * @param y The new Y position.
   */
  default void setPosition(float x, float y) {
    setX(x);
    setY(y);
  }

  /**
   * Shortcut for setting both {@link #getWidth()} and {@link #getHeight()}.
   *
   * @param width The new width.
   * @param height The new height.
   */
  void setSize(float width, float height);

  /** Adds {@code dx} to the current X position. */
  void changeX(float dx);

  /** Adds {@code dy} to the current Y position. */
  void changeY(float dy);

  /** Returns the center X coordinate of this object. */
  float getMidpointX();

  /** Returns the center Y coordinate of this object. */
  float getMidpointY();
}
