/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.functional;

/**
 * Spatial and kinematic surface shared by {@link me.stringdotjar.flixelgdx.FlixelObject}: world position, size, scroll
 * factors, velocity-based motion, and immovable toggling so motion tweens never need to cast to {@code FlixelObject}.
 *
 * @see me.stringdotjar.flixelgdx.FlixelObject
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

  /** Horizontal velocity in pixels per second. */
  float getVelocityX();

  /** Sets horizontal velocity in pixels per second. */
  void setVelocityX(float velocityX);

  /** Vertical velocity in pixels per second. */
  float getVelocityY();

  /** Sets vertical velocity in pixels per second. */
  void setVelocityY(float velocityY);

  /**
   * Sets both velocity components.
   *
   * @param vx Horizontal velocity.
   * @param vy Vertical velocity.
   */
  void setVelocity(float vx, float vy);

  /** Horizontal acceleration in pixels per second squared. */
  float getAccelerationX();

  /** Sets horizontal acceleration in pixels per second squared. */
  void setAccelerationX(float ax);

  /** Vertical acceleration in pixels per second squared. */
  float getAccelerationY();

  /** Sets vertical acceleration in pixels per second squared. */
  void setAccelerationY(float ay);

  /**
   * Sets both acceleration components.
   *
   * @param ax Horizontal acceleration.
   * @param ay Vertical acceleration.
   */
  void setAcceleration(float ax, float ay);

  /**
   * Deceleration applied when {@link #getAccelerationX()} is zero. Only applied when greater than {@code 0}.
   *
   * @return Horizontal drag.
   */
  float getDragX();

  /** Sets horizontal drag. */
  void setDragX(float dx);

  /**
   * Deceleration applied when {@link #getAccelerationY()} is zero. Only applied when greater than {@code 0}.
   *
   * @return Vertical drag.
   */
  float getDragY();

  /** Sets vertical drag. */
  void setDragY(float dy);

  /**
   * Sets both drag components.
   *
   * @param dx Horizontal drag.
   * @param dy Vertical drag.
   */
  void setDrag(float dx, float dy);

  /** Maximum absolute horizontal velocity. */
  float getMaxVelocityX();

  /** Sets maximum absolute horizontal velocity. */
  void setMaxVelocityX(float mvx);

  /** Maximum absolute vertical velocity. */
  float getMaxVelocityY();

  /** Sets maximum absolute vertical velocity. */
  void setMaxVelocityY(float mvy);

  /**
   * Sets both max velocity components.
   *
   * @param mvx Maximum horizontal speed.
   * @param mvy Maximum vertical speed.
   */
  void setMaxVelocity(float mvx, float mvy);

  /** Rotational speed in degrees per second. */
  float getAngularVelocity();

  /** Sets rotational speed in degrees per second. */
  void setAngularVelocity(float av);

  /** Rotational acceleration in degrees per second squared. */
  float getAngularAcceleration();

  /** Sets rotational acceleration in degrees per second squared. */
  void setAngularAcceleration(float aa);

  /** Rotational drag in degrees per second squared. */
  float getAngularDrag();

  /** Sets rotational drag in degrees per second squared. */
  void setAngularDrag(float ad);

  /** Maximum angular velocity in degrees per second. */
  float getMaxAngularVelocity();

  /** Sets maximum angular velocity in degrees per second. */
  void setMaxAngularVelocity(float mav);

  /**
   * When {@code true}, {@link me.stringdotjar.flixelgdx.FlixelObject#updateMotion(float)} runs each frame on
   * {@link me.stringdotjar.flixelgdx.FlixelObject}.
   *
   * @return Whether integrated motion is enabled.
   */
  boolean getMoves();

  /**
   * Enables or disables automatic motion integration on {@link me.stringdotjar.flixelgdx.FlixelObject}.
   *
   * @param moves {@code true} to integrate velocity each frame.
   */
  void setMoves(boolean moves);

  /**
   * When {@code true}, this object will not be moved by collision resolution. Other objects will still be pushed away
   * from it.
   *
   * @return Current immovable flag.
   */
  boolean isImmovable();

  /**
   * Sets whether collision resolution may move this object.
   *
   * @param immovable {@code true} to freeze this object during separation.
   */
  void setImmovable(boolean immovable);
}
