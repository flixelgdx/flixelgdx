/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.tween.type;

import java.util.Objects;

import com.badlogic.gdx.math.MathUtils;

import me.stringdotjar.flixelgdx.functional.FlixelShakeable;
import me.stringdotjar.flixelgdx.tween.FlixelTween;
import me.stringdotjar.flixelgdx.tween.settings.FlixelTweenSettings;
import me.stringdotjar.flixelgdx.util.FlixelAxes;

import org.jetbrains.annotations.Nullable;

/**
 * Random shake applied to a {@link FlixelShakeable} target by jittering its shake channel (for example sprite graphic
 * offset or world position) around values captured at {@link #start()}. Values are restored on {@link #reset()},
 * {@link #cancel()}, and when the tween is pooled.
 *
 * <p>Intensity can be interpreted as a fraction of {@link FlixelShakeable#getShakeWidth()} /
 * {@link FlixelShakeable#getShakeHeight()} (HaxeFlixel-style when those return positive sizes), or as plain
 * pixels (Unity or Godot style). When {@link ShakeUnit#FRACTION} is used and a size hook returns {@code 0} or less on an
 * axis, that axis falls back to treating intensity like {@link ShakeUnit#PIXELS}. Use {@link #setShakeUnit(ShakeUnit)} and
 * {@link #setFadeOut(boolean)} to configure behavior after {@link #setShake(FlixelShakeable, FlixelAxes, float)} or alongside
 * it.
 */
public class FlixelShakeTween extends FlixelTween {

  protected @Nullable FlixelShakeable target;
  protected FlixelAxes axes = FlixelAxes.XY;
  protected ShakeUnit shakeUnit = ShakeUnit.FRACTION;
  protected float intensity = 0.05f;
  protected boolean fadeOut = false;
  protected float savedX;
  protected float savedY;

  public FlixelShakeTween(@Nullable FlixelTweenSettings settings) {
    super(settings);
  }

  /**
   * Sets target, axes, and intensity. Does not change {@link #shakeUnit} or {@link #fadeOut}; those
   * stay at defaults from {@link #reset()} ({@link ShakeUnit#FRACTION}, fade out false) until you set
   * them with {@link #setShakeUnit(ShakeUnit)} or {@link #setFadeOut(boolean)}. Call order is free:
   * you may chain before or after this method.
   *
   * @param target The object whose shake channel is jittered.
   * @param axes Which axes receive random offset.
   * @param intensity With {@link ShakeUnit#FRACTION} and positive {@link FlixelShakeable#getShakeWidth()} /
   *   {@link FlixelShakeable#getShakeHeight()}, use a small value (for example 0.05f). With
   *   {@link ShakeUnit#PIXELS}, use half-range in pixels.
   * @return this tween for chaining.
   */
  public FlixelShakeTween setShake(@Nullable FlixelShakeable target, FlixelAxes axes, float intensity) {
    this.target = target;
    this.axes = axes != null ? axes : FlixelAxes.XY;
    this.intensity = intensity;
    return this;
  }

  /**
   * Sets how {@link #intensity} is interpreted. Default is {@link ShakeUnit#FRACTION}.
   *
   * @return this tween for chaining.
   */
  public FlixelShakeTween setShakeUnit(ShakeUnit shakeUnit) {
    this.shakeUnit = shakeUnit != null ? shakeUnit : ShakeUnit.FRACTION;
    return this;
  }

  /**
   * When true, shake strength tapers to zero as the tween progresses ({@code scale} toward 1).
   * When false, each frame uses the full random range until the tween ends.
   *
   * @return this tween for chaining.
   */
  public FlixelShakeTween setFadeOut(boolean fadeOut) {
    this.fadeOut = fadeOut;
    return this;
  }

  public ShakeUnit getShakeUnit() {
    return shakeUnit;
  }

  public boolean isFadeOut() {
    return fadeOut;
  }

  @Override
  public FlixelTween start() {
    super.start();
    if (target != null) {
      savedX = target.getShakeX();
      savedY = target.getShakeY();
    }
    return this;
  }

  @Override
  protected void updateTweenValues() {
    if (target == null) {
      return;
    }
    float taper = fadeOut ? (1f - scale) : 1f;
    float halfX = halfRangePixelsX();
    float halfY = halfRangePixelsY();
    float ix = (axes == FlixelAxes.Y) ? 0f : MathUtils.random(-halfX, halfX) * taper;
    float iy = (axes == FlixelAxes.X) ? 0f : MathUtils.random(-halfY, halfY) * taper;
    target.setShake(savedX + ix, savedY + iy);
  }

  /** Half-range in pixels for horizontal shake (0 if axis disabled). */
  private float halfRangePixelsX() {
    if (axes == FlixelAxes.Y) {
      return 0f;
    }
    if (shakeUnit == ShakeUnit.PIXELS) {
      return Math.max(0f, intensity);
    }
    if (target == null) {
      return Math.max(0f, intensity);
    }
    float w = target.getShakeWidth();
    if (w > 0f) {
      return Math.max(0f, intensity) * w;
    }
    return Math.max(0f, intensity);
  }

  /** Half-range in pixels for vertical shake (0 if axis disabled). */
  private float halfRangePixelsY() {
    if (axes == FlixelAxes.X) {
      return 0f;
    }
    if (shakeUnit == ShakeUnit.PIXELS) {
      return Math.max(0f, intensity);
    }
    if (target == null) {
      return Math.max(0f, intensity);
    }
    float h = target.getShakeHeight();
    if (h > 0f) {
      return Math.max(0f, intensity) * h;
    }
    return Math.max(0f, intensity);
  }

  @Override
  public void reset() {
    restoreShake();
    super.reset();
    target = null;
    axes = FlixelAxes.XY;
    shakeUnit = ShakeUnit.FRACTION;
    intensity = 0.05f;
    fadeOut = false;
    savedX = 0f;
    savedY = 0f;
  }

  protected void restoreShake() {
    if (target != null) {
      target.setShake(savedX, savedY);
    }
  }

  @Override
  public FlixelTween cancel() {
    restoreShake();
    return super.cancel();
  }

  @Override
  public boolean isTweenOf(Object object, String field) {
    if (target == null) {
      return false;
    }
    if (field == null || field.isEmpty()) {
      return Objects.equals(object, target);
    }
    return Objects.equals(object, target) && "shake".equals(field);
  }

  /**
   * How {@link FlixelShakeTween#intensity} maps to maximum offset per axis.
   *
   * <p>{@link #FRACTION} scales horizontal range by {@link FlixelShakeable#getShakeWidth()} and vertical range by
   * {@link FlixelShakeable#getShakeHeight()} when those return values greater than zero (per axis enabled by
   * {@link FlixelAxes}). Typical values are small, for example {@code 0.05f}.
   *
   * <p>{@link #PIXELS} sets the half-range in pixels on each active axis (random offset in {@code [-intensity, +intensity]}).
   */
  public enum ShakeUnit {

    /**
     * Scale intensity by {@link FlixelShakeable#getShakeWidth()} (X) and
     * {@link FlixelShakeable#getShakeHeight()} (Y) when positive. This is the default value.
     *
     * <p>Use this if you're familiar with HaxeFlixel and want to use the same behavior.
     */
    FRACTION,

    /**
     * Treat intensity as pixel magnitude on each shaken axis.
     *
     * <p>Use this if you want to directly set the half-range in pixels on each active
     * axis (random offset in {@code [-intensity, +intensity]}) and you're used to how
     * Unity or Godot shakes objects.
     */
    PIXELS
  }
}
