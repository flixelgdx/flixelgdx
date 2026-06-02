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
package org.flixelgdx.tween.type;

import java.util.Objects;
import java.util.function.Predicate;

import org.flixelgdx.functional.FlixelVisible;
import org.flixelgdx.tween.FlixelTween;
import org.flixelgdx.tween.settings.FlixelTweenSettings;

import org.jetbrains.annotations.Nullable;

/**
 * Toggles {@link FlixelVisible#isVisible()} over time; restores visibility on completion.
 */
public class FlixelFlickerTween extends FlixelTween {

  protected @Nullable FlixelVisible flickerTarget;
  protected float period = 0.08f;
  protected float ratio = 0.5f;
  protected boolean endVisibility = true;
  protected @Nullable Predicate<FlixelFlickerTween> tweenFunction;

  public FlixelFlickerTween(@Nullable FlixelTweenSettings settings) {
    super(settings);
  }

  public FlixelFlickerTween setFlicker(
      @Nullable FlixelVisible flickerTarget,
      float period,
      float ratio,
      boolean endVisibility,
      @Nullable Predicate<FlixelFlickerTween> tweenFunction) {
    this.flickerTarget = flickerTarget;
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
    if (flickerTarget == null || tweenSettings == null) {
      return;
    }
    float delay = tweenSettings.getStartDelay();
    if (secondsSinceStart < delay) {
      return;
    }
    Predicate<FlixelFlickerTween> fn = tweenFunction != null ? tweenFunction : FlixelFlickerTween::defaultTweenFunction;
    boolean vis = fn.test(this);
    if (flickerTarget.isVisible() != vis) {
      flickerTarget.setVisible(vis);
    }
  }

  @Override
  public void finish() {
    boolean looping = tweenSettings != null && tweenSettings.getType().isLooping();
    super.finish();
    if (!looping && flickerTarget != null) {
      flickerTarget.setVisible(endVisibility);
    }
  }

  @Override
  public boolean isTweenOf(Object object, String field) {
    if (flickerTarget == null) {
      return false;
    }
    if (field == null || field.isEmpty()) {
      return Objects.equals(object, flickerTarget);
    }
    return Objects.equals(object, flickerTarget)
        && ("visible".equals(field) || "flicker".equals(field));
  }

  @Override
  public void reset() {
    super.reset();
    flickerTarget = null;
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
