/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.tween.type;

import java.util.Objects;
import java.util.function.Predicate;

import me.stringdotjar.flixelgdx.FlixelBasic;
import me.stringdotjar.flixelgdx.tween.FlixelTween;
import me.stringdotjar.flixelgdx.tween.settings.FlixelTweenSettings;

import org.jetbrains.annotations.Nullable;

/**
 * Toggles {@link FlixelBasic#visible} over time; restores visibility on completion.
 */
public class FlixelFlickerTween extends FlixelTween {

  protected @Nullable FlixelBasic basic;
  protected float period = 0.08f;
  protected float ratio = 0.5f;
  protected boolean endVisibility = true;
  protected @Nullable Predicate<FlixelFlickerTween> tweenFunction;

  public FlixelFlickerTween(@Nullable FlixelTweenSettings settings) {
    super(settings);
  }

  public FlixelFlickerTween setFlicker(
      @Nullable FlixelBasic basic,
      float period,
      float ratio,
      boolean endVisibility,
      @Nullable Predicate<FlixelFlickerTween> tweenFunction) {
    this.basic = basic;
    this.period = period > 0f ? period : 1f / 60f;
    this.ratio = Math.max(0f, Math.min(1f, ratio));
    this.endVisibility = endVisibility;
    this.tweenFunction = tweenFunction;
    return this;
  }

  /**
   * The default tween function that toggles visibility based on the period and ratio.
   *
   * @param t The tween.
   * @return {@code true} if the object should be visible, {@code false} otherwise.
   */
  public static boolean defaultTweenFunction(FlixelFlickerTween t) {
    float p = t.period;
    if (p <= 0f) {
      return false;
    }
    float phase = (t.secondsSinceStart / p) % 1f;
    if (phase < 0f) {
      phase += 1f;
    }
    return phase > t.ratio;
  }

  @Override
  protected void updateTweenValues() {
    if (basic == null || tweenSettings == null) {
      return;
    }
    float delay = tweenSettings.getStartDelay();
    if (secondsSinceStart < delay) {
      return;
    }
    Predicate<FlixelFlickerTween> fn = tweenFunction != null ? tweenFunction : FlixelFlickerTween::defaultTweenFunction;
    boolean vis = fn.test(this);
    if (basic.visible != vis) {
      basic.visible = vis;
    }
  }

  @Override
  public void finish() {
    boolean looping = tweenSettings != null && tweenSettings.getType().isLooping();
    super.finish();
    if (!looping && basic != null) {
      basic.visible = endVisibility;
    }
  }

  @Override
  public boolean isTweenOf(Object object, String field) {
    if (basic == null) {
      return false;
    }
    if (field == null || field.isEmpty()) {
      return Objects.equals(object, basic);
    }
    return Objects.equals(object, basic)
        && ("visible".equals(field) || "flicker".equals(field));
  }

  @Override
  public void reset() {
    super.reset();
    basic = null;
    period = 0.08f;
    ratio = 0.5f;
    endVisibility = true;
    tweenFunction = null;
  }

  public float getSecondsSinceStart() {
    return secondsSinceStart;
  }

  public float getPeriod() {
    return period;
  }

  public float getRatio() {
    return ratio;
  }
}
