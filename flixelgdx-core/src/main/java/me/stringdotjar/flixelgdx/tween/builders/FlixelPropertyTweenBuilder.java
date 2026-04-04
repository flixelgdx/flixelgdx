/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.tween.builders;

import com.badlogic.gdx.utils.Array;

import me.stringdotjar.flixelgdx.functional.supplier.FloatSupplier;
import me.stringdotjar.flixelgdx.tween.FlixelTween;
import me.stringdotjar.flixelgdx.tween.settings.FlixelTweenSettings;
import me.stringdotjar.flixelgdx.tween.settings.FlixelTweenSettings.FlixelTweenPropertyGoal;
import me.stringdotjar.flixelgdx.tween.settings.FlixelTweenSettings.FlixelTweenPropertyGoal.FlixelTweenPropertyFloatGetter;
import me.stringdotjar.flixelgdx.tween.settings.FlixelTweenSettings.FlixelTweenPropertyGoal.FlixelTweenPropertyFloatSetter;
import me.stringdotjar.flixelgdx.tween.type.FlixelPropertyTween;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fluent builder for {@link FlixelPropertyTween}. Use getter/setter goals via {@link #addGoal}.
 * {@link #setObject(Object)} is required before {@link #start()}. Chain ends with {@link #start()}.
 */
public final class FlixelPropertyTweenBuilder extends FlixelAbstractTweenBuilder<FlixelPropertyTween, FlixelPropertyTweenBuilder> {

  private final Array<FlixelTweenSettings.FlixelTweenPropertyGoal> propertyGoals = new Array<>();
  private Object tweenObject;
  private @Nullable String fieldLabel;

  /** Creates a new property tween builder. Use {@link FlixelTween#tween(Class, Class) FlixelTween.tween(FlixelPropertyTween.class, FlixelPropertyTweenBuilder.class)}. */
  public FlixelPropertyTweenBuilder() {}

  @Override
  protected FlixelPropertyTweenBuilder self() {
    return this;
  }

  public FlixelPropertyTweenBuilder addGoal(@NotNull FlixelTweenPropertyFloatGetter getter, float toValue, @NotNull FlixelTweenPropertyFloatSetter setter) {
    propertyGoals.add(new FlixelTweenPropertyGoal(getter, toValue, setter));
    return this;
  }

  /**
   * Adds a property goal using the shared {@link FloatSupplier} interface.
   */
  public FlixelPropertyTweenBuilder addGoal(@NotNull FloatSupplier getter, float toValue, @NotNull FlixelTweenPropertyFloatSetter setter) {
    propertyGoals.add(new FlixelTweenPropertyGoal(getter, toValue, setter));
    return this;
  }

  /**
   * Required: the logical object this tween animates (for {@link FlixelPropertyTween#isTweenOf(Object, String)}).
   */
  public FlixelPropertyTweenBuilder setObject(@NotNull Object tweenObject) {
    this.tweenObject = tweenObject;
    return this;
  }

  /**
   * Optional label for {@link FlixelPropertyTween#isTweenOf(Object, String)} when matching field paths.
   */
  public FlixelPropertyTweenBuilder setFieldLabel(@Nullable String fieldLabel) {
    this.fieldLabel = fieldLabel;
    return this;
  }

  @Override
  public FlixelPropertyTween start() {
    if (tweenObject == null) {
      throw new IllegalStateException("FlixelPropertyTween requires setObject(Object) before start()");
    }
    FlixelTweenSettings settings = new FlixelTweenSettings(type, ease);
    applyTo(settings);
    for (int i = 0; i < propertyGoals.size; i++) {
      var goal = propertyGoals.get(i);
      settings.addGoal(goal.getter(), goal.toValue(), goal.setter());
    }
    FlixelPropertyTween tween =
        manager.obtainTween(FlixelPropertyTween.class, () -> new FlixelPropertyTween(settings));
    tween.setTweenSettings(settings);
    tween.setObject(tweenObject);
    tween.setFieldLabel(fieldLabel);

    return (FlixelPropertyTween) manager.addTween(tween);
  }
}
