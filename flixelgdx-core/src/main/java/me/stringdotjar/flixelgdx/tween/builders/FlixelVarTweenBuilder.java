/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.tween.builders;

import com.badlogic.gdx.utils.Array;
import me.stringdotjar.flixelgdx.tween.FlixelTween;
import me.stringdotjar.flixelgdx.tween.settings.FlixelTweenSettings;
import me.stringdotjar.flixelgdx.tween.type.FlixelVarTween;
import org.jetbrains.annotations.NotNull;

/**
 * Fluent builder for {@link FlixelVarTween} (reflection-based). Requires {@link #setObject(Object)} and
 * at least one {@link #addGoal(String, float)}. Chain ends with {@link #start()}.
 */
public final class FlixelVarTweenBuilder extends FlixelAbstractTweenBuilder<FlixelVarTween, FlixelVarTweenBuilder> {

  private final Array<FlixelTweenSettings.FlixelTweenVarGoal> goals = new Array<>();
  private Object object;

  /** Creates a new var tween builder. Use {@link FlixelTween#tween(Class, Class) FlixelTween.tween(FlixelVarTween.class, FlixelVarTweenBuilder.class)}. */
  public FlixelVarTweenBuilder() {}

  @Override
  protected FlixelVarTweenBuilder self() {
    return this;
  }

  /**
   * Sets the object to tween.
   *
   * @param object The object to tween.
   * @return The builder.
   */
  public FlixelVarTweenBuilder setObject(@NotNull Object object) {
    this.object = object;
    return this;
  }

  /**
   * Adds a goal to the tween.
   *
   * @param field The field to tween.
   * @param value The value to tween to.
   * @return The builder.
   */
  public FlixelVarTweenBuilder addGoal(@NotNull String field, float value) {
    goals.add(new FlixelTweenSettings.FlixelTweenVarGoal(field, value));
    return this;
  }

  @Override
  public FlixelVarTween start() {
    if (object == null) {
      throw new IllegalStateException("FlixelVarTween requires setObject(object) before start()");
    }
    if (goals.isEmpty()) {
      throw new IllegalStateException("FlixelVarTween requires at least one addGoal(field, value) before start()");
    }
    FlixelTweenSettings settings = new FlixelTweenSettings(type, ease);
    applyTo(settings);
    for (int i = 0; i < goals.size; i++) {
      var goal = goals.get(i);
      settings.addGoal(goal.field(), goal.value());
    }
    FlixelVarTween tween = manager.obtainTween(FlixelVarTween.class, () -> new FlixelVarTween(object, settings));
    tween.setTweenSettings(settings);
    tween.setObject(object);
    return (FlixelVarTween) manager.addTween(tween);
  }
}
