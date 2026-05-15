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

import me.stringdotjar.flixelgdx.tween.FlixelTween;
import me.stringdotjar.flixelgdx.tween.settings.FlixelTweenSettings;

import org.jetbrains.annotations.Nullable;

/**
 * Tween type for animating any object via lambda getters and setters.
 *
 * <p>It's intended to be used the following way:
 *
 * <pre>{@code
 * // Use the FlixelTween class to use this type.
 * FlixelTween.tween(myObject, new FlixelTweenSettings()
 *   .addGoal(myObject::getFoo, 100f, myObject::setFoo)
 *   .addGoal(myObject::getBar, 200f, myObject::setBar));
 * }
 *
 * <p>If you need to cancel a specific tween of an object, you can add a label to the tween
 * via {@link #setFieldLabel(String)} and use that label in the query:
 *
 * <pre>{@code
 * FlixelGoalTween t = (FlixelGoalTween) FlixelTween.tween(myObject, new FlixelTweenSettings()
 *   .addGoal(myObject::getFoo, 100f, myObject::setFoo)
 *   .addGoal(myObject::getBar, 200f, myObject::setBar));
 *
 * t.setFieldLabel("yourLabel");
 *
 * // Later in your game's code...
 * FlixelTween.cancelTweensOf(myObject, "yourLabel");
 * FlixelTween.completeTweensOf(myObject, "yourLabel");
 * }
 *
 * @see FlixelTweenSettings
 */
public class FlixelGoalTween extends FlixelTween {

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
  protected Array<FlixelTweenSettings.FlixelTweenGoal> cachedPropertyGoals = new Array<>();

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
  public FlixelGoalTween(FlixelTweenSettings settings) {
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
  public FlixelGoalTween setObject(@Nullable Object tweenObject) {
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
  public FlixelGoalTween setFieldLabel(@Nullable String fieldLabel) {
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
      throw new IllegalStateException("FlixelGoalTween requires setObject(Object) before start(). "
        + "Use FlixelTween.tween(object, new FlixelTweenSettings()...) or call setObject(...) after obtaining the tween from the pool.");
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

  /**
   * Returns whether this tween matches the given object and optional field path.
   *
   * <p>When {@code field} contains no {@code '.'}, the match requires object identity with
   * {@link #setObject(Object)} and, if {@link #setFieldLabel(String)} was used, equality between
   * {@code field} and that label.
   *
   * <p>When {@code field} is dotted, the framework no longer walks nested objects. Matching succeeds
   * only when {@code o} is the same instance as the logical tween object and the segment after the
   * final {@code '.'} equals {@code fieldLabel} when a label is configured (or any suffix when the
   * label is {@code null}). Callers that cancel tweens using a root object and a long dotted path
   * should pass the same object reference that was passed to {@code setObject(...)}.
   *
   * @param o Candidate object from the manager query.
   * @param field Optional path string (may contain dots).
   * @return {@code true} when this tween should be treated as a match.
   */
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
    int lastDot = field.lastIndexOf('.');
    String leaf = field.substring(lastDot + 1);
    boolean leafOk = fieldLabel == null || fieldLabel.equals(leaf);
    return leafOk && Objects.equals(o, tweenObject);
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
