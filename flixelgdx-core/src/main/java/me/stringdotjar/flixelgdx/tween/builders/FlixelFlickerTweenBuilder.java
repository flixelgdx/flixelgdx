/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.tween.builders;

import java.util.function.Predicate;

import me.stringdotjar.flixelgdx.FlixelBasic;
import me.stringdotjar.flixelgdx.tween.settings.FlixelTweenSettings;
import me.stringdotjar.flixelgdx.tween.type.FlixelFlickerTween;

import org.jetbrains.annotations.Nullable;

/** Builder for {@link FlixelFlickerTween}. */
public final class FlixelFlickerTweenBuilder extends FlixelAbstractTweenBuilder<FlixelFlickerTween, FlixelFlickerTweenBuilder> {

  private @Nullable FlixelBasic basic;
  private float period = 0.08f;
  private float ratio = 0.5f;
  private boolean endVisibility = true;
  private @Nullable Predicate<FlixelFlickerTween> tweenFunction;

  @Override
  protected FlixelFlickerTweenBuilder self() {
    return this;
  }

  public FlixelFlickerTweenBuilder setBasic(@Nullable FlixelBasic basic) {
    this.basic = basic;
    return this;
  }

  public FlixelFlickerTweenBuilder setPeriod(float period) {
    this.period = period;
    return this;
  }

  public FlixelFlickerTweenBuilder setRatio(float ratio) {
    this.ratio = ratio;
    return this;
  }

  public FlixelFlickerTweenBuilder setEndVisibility(boolean endVisibility) {
    this.endVisibility = endVisibility;
    return this;
  }

  public FlixelFlickerTweenBuilder setTweenFunction(@Nullable Predicate<FlixelFlickerTween> tweenFunction) {
    this.tweenFunction = tweenFunction;
    return this;
  }

  @Override
  public FlixelFlickerTween start() {
    FlixelTweenSettings settings = new FlixelTweenSettings(type, ease);
    applyTo(settings);
    FlixelFlickerTween tween =
        manager.obtainTween(FlixelFlickerTween.class, () -> new FlixelFlickerTween(settings));
    tween.setTweenSettings(settings);
    tween.setFlicker(basic, period, ratio, endVisibility, tweenFunction);
    return (FlixelFlickerTween) manager.addTween(tween);
  }
}
