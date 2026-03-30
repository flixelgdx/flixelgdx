/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.tween.type;

import java.util.Objects;

import com.badlogic.gdx.math.MathUtils;

import me.stringdotjar.flixelgdx.FlixelSprite;
import me.stringdotjar.flixelgdx.tween.FlixelTween;
import me.stringdotjar.flixelgdx.tween.settings.FlixelTweenSettings;
import me.stringdotjar.flixelgdx.util.FlixelAxes;

import org.jetbrains.annotations.Nullable;

/**
 * Random shake applied to a sprite by adjusting {@link FlixelSprite#getOffsetX()} and
 * {@link FlixelSprite#getOffsetY()}. Initial offsets are stored at {@link #start()} and restored on
 * {@link #reset()}, {@link #cancel()}, and when the tween is pooled.
 *
 * <p>Intensity can be interpreted as a fraction of the sprite size (HaxeFlixel-style) or as plain
 * pixels (similar to Unity or Godot). Use {@link #setShakeUnit(ShakeUnit)} and {@link #setFadeOut(boolean)} to configure
 * behavior after {@link #setShake(FlixelSprite, FlixelAxes, float)} or alongside it.
 */
public class FlixelShakeTween extends FlixelTween {

  protected @Nullable FlixelSprite sprite;
  protected FlixelAxes axes = FlixelAxes.XY;
  protected ShakeUnit shakeUnit = ShakeUnit.FRACTION;
  protected float intensity = 0.05f;
  protected boolean fadeOut = false;
  protected float savedOffsetX;
  protected float savedOffsetY;

  public FlixelShakeTween(@Nullable FlixelTweenSettings settings) {
    super(settings);
  }

  /**
   * Sets sprite, axes, and intensity. Does not change {@link #shakeUnit} or {@link #fadeOut}; those
   * stay at defaults from {@link #reset()} ({@link ShakeUnit#FRACTION}, fade out false) until you set
   * them with {@link #setShakeUnit(ShakeUnit)} or {@link #setFadeOut(boolean)}. Call order is free:
   * you may chain before or after this method.
   *
   * @param sprite The sprite whose offsets are jittered.
   * @param axes Which axes receive random offset.
   * @param intensity With {@link ShakeUnit#FRACTION}, use a small value (for example 0.05f). With
   *     {@link ShakeUnit#PIXELS}, use half-range in pixels.
   * @return this tween for chaining.
   */
  public FlixelShakeTween setShake(@Nullable FlixelSprite sprite, FlixelAxes axes, float intensity) {
    this.sprite = sprite;
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
    if (sprite != null) {
      savedOffsetX = sprite.getOffsetX();
      savedOffsetY = sprite.getOffsetY();
    }
    return this;
  }

  @Override
  protected void updateTweenValues() {
    if (sprite == null) {
      return;
    }
    float taper = fadeOut ? (1f - scale) : 1f;
    float halfX = halfRangePixelsX();
    float halfY = halfRangePixelsY();
    float ix = (axes == FlixelAxes.Y) ? 0f : MathUtils.random(-halfX, halfX) * taper;
    float iy = (axes == FlixelAxes.X) ? 0f : MathUtils.random(-halfY, halfY) * taper;
    sprite.setOffset(savedOffsetX + ix, savedOffsetY + iy);
  }

  /** Half-range in pixels for horizontal shake (0 if axis disabled). */
  private float halfRangePixelsX() {
    if (axes == FlixelAxes.Y) {
      return 0f;
    }
    if (shakeUnit == ShakeUnit.PIXELS) {
      return Math.max(0f, intensity);
    }
    return Math.max(0f, intensity) * Math.max(0f, sprite.getWidth());
  }

  /** Half-range in pixels for vertical shake (0 if axis disabled). */
  private float halfRangePixelsY() {
    if (axes == FlixelAxes.X) {
      return 0f;
    }
    if (shakeUnit == ShakeUnit.PIXELS) {
      return Math.max(0f, intensity);
    }
    return Math.max(0f, intensity) * Math.max(0f, sprite.getHeight());
  }

  @Override
  public void reset() {
    restoreOffset();
    super.reset();
    sprite = null;
    axes = FlixelAxes.XY;
    shakeUnit = ShakeUnit.FRACTION;
    intensity = 0.05f;
    fadeOut = false;
    savedOffsetX = 0f;
    savedOffsetY = 0f;
  }

  protected void restoreOffset() {
    if (sprite != null) {
      sprite.setOffset(savedOffsetX, savedOffsetY);
    }
  }

  @Override
  public FlixelTween cancel() {
    restoreOffset();
    return super.cancel();
  }

  @Override
  public boolean isTweenOf(Object object, String field) {
    if (sprite == null) {
      return false;
    }
    if (field == null || field.isEmpty()) {
      return Objects.equals(object, sprite);
    }
    return Objects.equals(object, sprite) && "shake".equals(field);
  }

  /**
   * How {@link FlixelShakeTween#intensity} maps to maximum offset per axis.
   *
   * <p>{@link #FRACTION} is the default and horizontal range is roughly
   * {@code intensity * sprite width} and vertical range is {@code intensity * sprite height} (per
   * axis enabled by {@link FlixelAxes}). Typical values are small, for example {@code 0.05f}.
   *
   * <p>{@link #PIXELS} is used to directly set the half-range in pixels on each active
   * axis (random offset in {@code [-intensity, +intensity]}).
   */
  public enum ShakeUnit {

    /**
     * Scale intensity by sprite width (X) and height (Y). This is the default value.
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
