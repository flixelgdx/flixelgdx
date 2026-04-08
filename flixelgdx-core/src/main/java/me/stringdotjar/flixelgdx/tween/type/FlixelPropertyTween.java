/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.tween.type;

import java.util.Objects;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;

import me.stringdotjar.flixelgdx.Flixel;
import me.stringdotjar.flixelgdx.backend.reflect.FlixelPropertyPath;
import me.stringdotjar.flixelgdx.functional.supplier.FloatSupplier;
import me.stringdotjar.flixelgdx.tween.FlixelTween;
import me.stringdotjar.flixelgdx.tween.settings.FlixelTweenSettings;

import org.jetbrains.annotations.Nullable;

/**
 * Tween type for animating values via getter/setter pairs (property goals)
 * rather than reflection. Use this when you need setter side effects (for
 * example, bounds updates or listeners) to run on every interpolated step.
 * Configure property goals with
 * {@link FlixelTweenSettings#addGoal(FloatSupplier, float, FlixelTweenSettings.FlixelTweenPropertyGoal.FlixelTweenPropertyFloatSetter)}.
 *
 * <p>This tween type is faster than {@link FlixelVarTween}, which resolves
 * property names through {@link Flixel#reflect} on each frame. Both can
 * invoke JavaBean setters on every step when configured that way; prefer this
 * type when you can close over getter/setter references and avoid reflection.
 *
 * <h2>Recommended for Web / TeaVM Targets</h2>
 *
 * <p><strong>This is the recommended tween type for games targeting the web
 * (TeaVM) backend.</strong> Because it uses explicit getter/setter lambda
 * references instead of runtime reflection, it is fully compatible with
 * ahead-of-time compilation targets such as TeaVM. If your game targets the
 * web, always prefer this type over {@link FlixelVarTween}.
 *
 * @see FlixelVarTween
 * @see FlixelTweenSettings
 */
public class FlixelPropertyTween extends FlixelTween {

  /**
   * Logical subject for {@link #isTweenOf(Object, String)}; must be set before {@link #start()} /
   * {@link me.stringdotjar.flixelgdx.tween.FlixelTweenManager#addTween(FlixelTween)}.
   */
  protected @Nullable Object tweenObject;

  /** Optional label for {@link #isTweenOf(Object, String)} when no intrinsic property name exists. */
  protected @Nullable String fieldLabel;

  /**
   * Cached property goals captured at {@link #start()} to avoid re-allocating the list every
   * frame inside {@link #updateTweenValues()}.
   */
  protected Array<FlixelTweenSettings.FlixelTweenPropertyGoal> cachedPropertyGoals = new Array<>();

  /**
   * Initial values of each property goal, captured from their getter at {@link #start()}, indexed
   * parallel to {@link #cachedPropertyGoals}.
   */
  protected FloatArray propertyGoalStartValues = new FloatArray();

  /**
   * Constructs a new property tween with the given settings. Property goals must be added via
   * {@link FlixelTweenSettings#addGoal} before starting.
   *
   * @param settings The settings that configure and determine how the tween should animate.
   */
  public FlixelPropertyTween(FlixelTweenSettings settings) {
    super(settings);
  }

  /**
   * Sets the object {@code this} tween logically animates (required before {@link #start()}).
   *
   * <p>This has to be set because {@link #isTweenOf(Object, String)} needs to know the object to tween.
   * This method is purely for logic purposes used by {@link me.stringdotjar.flixelgdx.tween.FlixelTweenManager}, not
   * for tweening purposes.
   *
   * @param tweenObject The object to tween.
   * @return {@code this} for chaining.
   */
  public FlixelPropertyTween setObject(@Nullable Object tweenObject) {
    this.tweenObject = tweenObject;
    return this;
  }

  /**
   * Assigns an optional logical field name used by
   * {@link #isTweenOf(Object, String)} when checking whether this tween
   * animates a particular named property.
   *
   * @param fieldLabel The field label to associate with this tween, or {@code null} to clear any previously set label.
   * @return This tween instance for method chaining.
   */
  public FlixelPropertyTween setFieldLabel(@Nullable String fieldLabel) {
    this.fieldLabel = fieldLabel;
    return this;
  }

  /**
   * Returns the logical target object that this tween animates, or
   * {@code null} if no object has been set yet.
   *
   * @return The tween target object, or {@code null}.
   */
  @Nullable
  public Object getTweenObject() {
    return tweenObject;
  }

  /**
   * Returns the optional logical field label associated with this tween, or
   * {@code null} if none has been set.
   *
   * @return The field label, or {@code null}.
   */
  @Nullable
  public String getFieldLabel() {
    return fieldLabel;
  }

  @Override
  public FlixelTween start() {
    if (tweenObject == null) {
      throw new IllegalStateException(
          "FlixelPropertyTween requires setObject(Object) before start(). "
              + "Use FlixelTween.tween(FlixelPropertyTween.class, FlixelPropertyTweenBuilder.class) and call setObject on the builder.");
    }
    super.start();

    if (tweenSettings == null) {
      return this;
    }

    var propertyGoals = tweenSettings.getPropertyGoals();
    if (propertyGoals == null || propertyGoals.isEmpty()) {
      return this;
    }
    resetGoals();
    return this;
  }

  @Override
  protected void updateTweenValues() {
    for (int i = 0; i < cachedPropertyGoals.size; i++) {
      float startValue = propertyGoalStartValues.get(i);
      var goal = cachedPropertyGoals.get(i);
      if (goal == null) {
        continue;
      }
      float newValue = startValue + (goal.toValue() - startValue) * scale;
      goal.setter().set(newValue);
    }
  }

  @Override
  public void restart() {
    // For manual restarts, refresh the starting values from the current object state
    // so the tween resumes from "where things are now". For internal loop / ping-pong
    // restarts, keep the original start values so the animation stays between the
    // original endpoints.
    if (!internalRestart && tweenSettings != null) {
      var propertyGoals = tweenSettings.getPropertyGoals();
      if (propertyGoals != null && !propertyGoals.isEmpty()) {
        resetGoals();
      }
    }
    super.restart();
  }

  @Override
  public void reset() {
    super.reset();
    cachedPropertyGoals.clear();
    propertyGoalStartValues.clear();
    tweenObject = null;
    fieldLabel = null;
  }

  @Override
  public boolean isTweenOf(Object o, String field) {
    if (tweenObject == null) {
      return false;
    }
    if (field == null || field.isEmpty()) {
      return Objects.equals(o, tweenObject);
    }
    if (field.indexOf('.') < 0) {
      return Objects.equals(o, tweenObject) && (fieldLabel == null || fieldLabel.equals(field));
    }
    FlixelPropertyPath path = Flixel.reflect.resolvePropertyPath(o, field);
    return Objects.equals(path.leafObject(), tweenObject)
        && (fieldLabel == null || fieldLabel.equals(path.leafName()));
  }

  private void resetGoals() {
    var propertyGoals = tweenSettings.getPropertyGoals();
    cachedPropertyGoals.clear();
    propertyGoalStartValues.clear();
    for (int i = 0; i < propertyGoals.size; i++) {
      var goal = propertyGoals.get(i);
      if (goal == null) {
        continue;
      }
      cachedPropertyGoals.add(goal);
      propertyGoalStartValues.add(goal.getter().getAsFloat());
    }
  }
}
