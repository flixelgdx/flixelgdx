/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.tween.settings;

import com.badlogic.gdx.utils.Array;

import me.stringdotjar.flixelgdx.tween.FlixelTween;
import me.stringdotjar.flixelgdx.functional.supplier.FloatSupplier;
import me.stringdotjar.flixelgdx.tween.FlixelEase;
import me.stringdotjar.flixelgdx.tween.type.FlixelVarTween;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Class for holding basic data, containing configurations to be used on a {@link FlixelTween}.
 */
public class FlixelTweenSettings {

  private float duration;
  private float startDelay;
  private float loopDelay;
  private float framerate;
  private FlixelTweenType type;
  private FlixelEase.FunkinEaseFunction ease;
  private FlixelEase.FunkinEaseStartCallback onStart;
  private FlixelEase.FunkinEaseUpdateCallback onUpdate;
  private FlixelEase.FunkinEaseCompleteCallback onComplete;
  private Array<FlixelTweenVarGoal> goals;
  private Array<FlixelTweenPropertyGoal> propertyGoals;

  public FlixelTweenSettings() {
    this(FlixelTweenType.ONESHOT, FlixelEase::linear);
  }

  /**
   * @param type The type of tween it should be.
   */
  public FlixelTweenSettings(@NotNull FlixelTweenType type) {
    this(type, FlixelEase::linear);
  }

  /**
   * @param type The type of tween it should be.
   * @param ease The easer function the tween should use (aka how it should be animated).
   */
  public FlixelTweenSettings(
    @NotNull FlixelTweenType type,
    @Nullable FlixelEase.FunkinEaseFunction ease) {
    this.duration = 1.0f;
    this.startDelay = 0.0f;
    this.loopDelay = 0.0f;
    this.framerate = 0.0f;
    this.type = type;
    this.ease = ease;
    this.onStart = null;
    this.onUpdate = null;
    this.onComplete = null;
    this.goals = new Array<>();
    this.propertyGoals = new Array<>();
  }

  /**
   * Adds a new goal to tween an objects field value to using reflection.
   *
   * <p>Note that this is only used on a {@link me.stringdotjar.flixelgdx.tween.type.FlixelVarTween}.
   *
   * @param field The field to tween.
   * @param value The value to tween the field to.
   * @return {@code this} tween settings object for chaining.
   */
  public FlixelTweenSettings addGoal(String field, float value) {
    goals.add(new FlixelTweenVarGoal(field, value));
    return this;
  }

  /**
   * Adds a new property goal that tweens a value via a getter and setter rather than direct field
   * access. This allows side effects (bounds updates, listeners, etc.) to fire naturally through
   * the setter on every interpolated step.
   *
   * <p>The getter is called once at tween start to capture the initial value. Each subsequent
   * update interpolates from that captured value toward {@code toValue} and passes the result to
   * the setter.
   *
   * @param getter Supplies the current value of the property at tween start.
   * @param toValue The value to tween the property to.
   * @param setter Consumes the interpolated value on every tween update.
   * @return {@code this} tween settings object for chaining.
   */
  public FlixelTweenSettings addGoal(@NotNull FlixelTweenPropertyGoal.FlixelTweenPropertyFloatGetter getter,
                                     float toValue,
                                     @NotNull FlixelTweenPropertyGoal.FlixelTweenPropertyFloatSetter setter) {
    propertyGoals.add(new FlixelTweenPropertyGoal(getter, toValue, setter));
    return this;
  }

  /**
   * Adds a new property goal using the shared {@link FloatSupplier} interface.
   *
   * @param getter Supplies the current value of the property at tween start.
   * @param toValue The value to tween the property to.
   * @param setter Consumes the interpolated value on every tween update.
   * @return {@code this} tween settings object for chaining.
   */
  public FlixelTweenSettings addGoal(@NotNull FloatSupplier getter,
                                     float toValue,
                                     @NotNull FlixelTweenPropertyGoal.FlixelTweenPropertyFloatSetter setter) {
    propertyGoals.add(new FlixelTweenPropertyGoal(getter, toValue, setter));
    return this;
  }

  /**
   * Sets the duration of how long the tween should last for.
   *
   * @param duration The new value to set.
   * @return {@code this} tween settings object for chaining.
   */
  public FlixelTweenSettings setDuration(float duration) {
    this.duration = duration;
    return this;
  }

  public float getDuration() {
    return duration;
  }

  public FlixelTweenType getType() {
    return type;
  }

  public FlixelEase.FunkinEaseFunction getEase() {
    return ease;
  }

  public FlixelEase.FunkinEaseStartCallback getOnStart() {
    return onStart;
  }

  public FlixelEase.FunkinEaseUpdateCallback getOnUpdate() {
    return onUpdate;
  }

  public FlixelEase.FunkinEaseCompleteCallback getOnComplete() {
    return onComplete;
  }

  public FlixelTweenVarGoal getGoal(String field) {
    for (FlixelTweenVarGoal goal : goals) {
      if (goal.field().equals(field)) {
        return goal;
      }
    }
    return null;
  }

  public Array<FlixelTweenVarGoal> getGoals() {
    return goals;
  }

  public Array<FlixelTweenPropertyGoal> getPropertyGoals() {
    return propertyGoals;
  }

  public float getLoopDelay() {
    return loopDelay;
  }

  public float getStartDelay() {
    return startDelay;
  }

  public void forEachGoal(FlixelTweenGoalVisitor visitor) {
    for (int i = 0; i < goals.size; i++) {
      var goal = goals.get(i);
      if (goal == null) {
        continue;
      }
      visitor.visit(goal.field(), goal.value());
    }
  }

  public float getFramerate() {
    return framerate;
  }

  public FlixelTweenSettings setEase(FlixelEase.FunkinEaseFunction ease) {
    this.ease = ease;
    return this;
  }

  public void clearGoals() {
    goals.clear();
    propertyGoals.clear();
  }

  public FlixelTweenSettings setStartDelay(float startDelay) {
    this.startDelay = startDelay;
    return this;
  }

  public FlixelTweenSettings setLoopDelay(float loopDelay) {
    this.loopDelay = loopDelay;
    return this;
  }

  public FlixelTweenSettings setFramerate(float framerate) {
    this.framerate = framerate;
    return this;
  }

  public FlixelTweenSettings setType(@NotNull FlixelTweenType type) {
    this.type = type;
    return this;
  }

  public FlixelTweenSettings setOnStart(FlixelEase.FunkinEaseStartCallback onStart) {
    this.onStart = onStart;
    return this;
  }

  public FlixelTweenSettings setOnUpdate(FlixelEase.FunkinEaseUpdateCallback onUpdate) {
    this.onUpdate = onUpdate;
    return this;
  }

  public FlixelTweenSettings setOnComplete(FlixelEase.FunkinEaseCompleteCallback onComplete) {
    this.onComplete = onComplete;
    return this;
  }

  /**
   * A record containing basic info for a {@link FlixelVarTween} goal (aka a field to tween a numeric value to).
   *
   * @param field The field to tween.
   * @param value The value to tween the field to.
   */
  public record FlixelTweenVarGoal(@NotNull String field, float value) {}

  /**
   * A record containing a getter, a target value, and a setter for a property-based tween goal.
   * Unlike {@link FlixelTweenVarGoal}, this does not rely on reflection; the caller supplies how to
   * read the initial value and how to apply each interpolated step, so setter side-effects fire
   * naturally on every update.
   *
   * @param getter Supplies the initial value of the property when the tween starts.
   * @param toValue The value to tween the property to.
   * @param setter Consumes the interpolated value on every tween update.
   */
  public record FlixelTweenPropertyGoal(@NotNull FloatSupplier getter, float toValue, @NotNull FlixelTweenPropertyFloatSetter setter) {

    /** Supplies a primitive {@code float} without boxing. */
    @FunctionalInterface
    public interface FlixelTweenPropertyFloatGetter extends FloatSupplier {
      float get();

      @Override
      default float getAsFloat() {
        return get();
      }
    }

    /** Consumes a primitive {@code float} without boxing. */
    @FunctionalInterface
    public interface FlixelTweenPropertyFloatSetter {
      void set(float value);
    }
  }

  @FunctionalInterface
  public interface FlixelTweenGoalVisitor {
    void visit(String field, float value);
  }
}
