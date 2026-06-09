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
package org.flixelgdx.tween.type.motion;

import org.flixelgdx.functional.FlixelPositional;
import org.flixelgdx.tween.FlixelTween;
import org.flixelgdx.tween.settings.FlixelTweenSettings;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Base class for motion tweens that drive an optional {@link FlixelPositional} position. When a target is set, it is
 * marked {@linkplain FlixelPositional#setImmovable(boolean) immovable} while the tween runs and restored on end.
 *
 * <p>Subclasses should create a {@code setMotion()} method that sets the motion for the tween.
 */
public abstract class FlixelMotion extends FlixelTween {

  /** Current world X for this motion. */
  public float motionX;

  /** Current world Y for this motion. */
  public float motionY;

  protected @Nullable FlixelPositional motionTarget;
  private boolean priorImmovable;
  private boolean immovableCaptured;

  protected FlixelMotion(@Nullable FlixelTweenSettings settings) {
    super(settings);
  }

  /**
   * Binds the object to move; captures and forces {@code immovable} for the tween lifetime.
   *
   * @param target The object to move.
   * @return {@code this} for chaining.
   */
  public FlixelMotion setMotionObject(@Nullable FlixelPositional target) {
    clearImmovableCapture();
    this.motionTarget = target;
    if (target != null) {
      priorImmovable = target.isImmovable();
      target.setImmovable(true);
      immovableCaptured = true;
    }
    return this;
  }

  public @Nullable FlixelPositional getMotionTarget() {
    return motionTarget;
  }

  /** Restores prior immovable flag on the current target without clearing the reference. */
  protected void clearImmovableCapture() {
    if (motionTarget != null && immovableCaptured) {
      motionTarget.setImmovable(priorImmovable);
    }
    immovableCaptured = false;
  }

  @Override
  protected void updateTweenValues() {
    computeMotion();
    if (motionTarget != null) {
      motionTarget.setPosition(motionX, motionY);
    }
  }

  /**
   * Computes and updates the motion for the tween.
   */
  protected abstract void computeMotion();

  @Override
  public void finish() {
    super.finish();
    if (tweenSettings != null && !tweenSettings.getType().isLooping()) {
      clearImmovableCapture();
      motionTarget = null;
    }
  }

  @Override
  public void reset() {
    clearImmovableCapture();
    motionTarget = null;
    super.reset();
  }

  @Override
  public boolean isTweenOf(Object object, String field) {
    if (motionTarget == null) {
      return false;
    }
    if (field == null || field.isEmpty()) {
      return Objects.equals(object, motionTarget);
    }
    return Objects.equals(object, motionTarget) && ("x".equals(field) || "y".equals(field));
  }
}
