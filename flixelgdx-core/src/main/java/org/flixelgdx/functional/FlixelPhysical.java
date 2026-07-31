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
 * Kinematic physics contract that extends {@link FlixelPositional} with velocity, acceleration, drag,
 * max velocity, angular motion, and collision-immovable control. This is the full motion surface shared
 * by {@link org.flixelgdx.FlixelObject FlixelObject}.
 *
 * <p>Motion tweens and physics code that need both position and kinematics should accept this type
 * rather than {@link FlixelPositional}, which covers only spatial layout.
 *
 * @see org.flixelgdx.FlixelObject
 * @see FlixelPositional
 */
public interface FlixelPhysical extends FlixelPositional {

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
   * When {@code true}, {@link org.flixelgdx.FlixelObject#updateMotion(float) FlixelObject.updateMotion(float)} runs each frame on
   * {@link org.flixelgdx.FlixelObject FlixelObject}.
   *
   * @return Whether integrated motion is enabled.
   */
  boolean getMoves();

  /**
   * Enables or disables automatic motion integration on {@link org.flixelgdx.FlixelObject FlixelObject}.
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
