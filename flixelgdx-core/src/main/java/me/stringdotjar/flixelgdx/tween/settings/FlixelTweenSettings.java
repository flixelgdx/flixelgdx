/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.tween.settings;

import com.badlogic.gdx.utils.Array;

import me.stringdotjar.flixelgdx.functional.supplier.FloatSupplier;
import me.stringdotjar.flixelgdx.tween.FlixelTween;
import me.stringdotjar.flixelgdx.tween.FlixelEase;
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
  private final Array<FlixelTweenGoal> goals;

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
    this.goals = new Array<>(false, 16);
  }

  /**
   * Adds a new tween goal that tweens a value via a getter and setter.
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
  public FlixelTweenSettings addGoal(@NotNull FlixelTweenSettings.FlixelTweenGoal.FlixelTweenGoalGetter getter,
                                     float toValue,
                                     @NotNull FlixelTweenSettings.FlixelTweenGoal.FlixelTweenGoalSetter setter) {
    goals.add(new FlixelTweenGoal(getter, toValue, setter));
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

  public Array<FlixelTweenGoal> getGoals() {
    return goals;
  }

  public float getLoopDelay() {
    return loopDelay;
  }

  public float getStartDelay() {
    return startDelay;
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

  public FlixelTweenSettings forEachGoal(FlixelTweenGoalVisitor visitor) {
    for (FlixelTweenGoal goal : goals) {
      visitor.visit(goal);
    }
    return this;
  }

  /**
   * A record containing a getter, a target value, and a setter for a property-based tween goal.
   *
   * @param getter Supplies the initial value of the property when the tween starts.
   * @param toValue The value to tween the property to.
   * @param setter Consumes the interpolated value on every tween update.
   */
  public record FlixelTweenGoal(@NotNull FloatSupplier getter, float toValue, @NotNull FlixelTweenSettings.FlixelTweenGoal.FlixelTweenGoalSetter setter) {

    /** Supplies a primitive {@code float} without boxing. */
    @FunctionalInterface
    public interface FlixelTweenGoalGetter extends FloatSupplier {
      float get();

      @Override
      default float getAsFloat() {
        return get();
      }
    }

    /** Consumes a primitive {@code float} without boxing. */
    @FunctionalInterface
    public interface FlixelTweenGoalSetter {
      void set(float value);
    }
  }

  /**
   * A functional interface for visiting each goal in {@code this} tween settings object.
   */
  @FunctionalInterface
  public interface FlixelTweenGoalVisitor {
    void visit(FlixelTweenGoal goal);
  }
}
