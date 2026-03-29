/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.tween.type.motion;

import java.util.Objects;

import me.stringdotjar.flixelgdx.FlixelObject;
import me.stringdotjar.flixelgdx.tween.FlixelTween;
import me.stringdotjar.flixelgdx.tween.settings.FlixelTweenSettings;

import org.jetbrains.annotations.Nullable;

/**
 * Base class for motion tweens that drive an optional {@link FlixelObject} position. While active, the
 * target is marked {@linkplain FlixelObject#setImmovable(boolean) immovable} and restored on end.
 *
 * <p>Subclasses should create a {@code setMotion()} method that sets the motion for the tween.
 */
public abstract class FlixelMotion extends FlixelTween {

  /** Current world X for this motion. */
  public float motionX;

  /** Current world Y for this motion. */
  public float motionY;

  protected @Nullable FlixelObject motionTarget;
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
  public FlixelMotion setMotionObject(@Nullable FlixelObject target) {
    clearImmovableCapture();
    this.motionTarget = target;
    if (target != null) {
      priorImmovable = target.isImmovable();
      target.setImmovable(true);
      immovableCaptured = true;
    }
    return this;
  }

  public @Nullable FlixelObject getMotionTarget() {
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
