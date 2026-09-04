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
package org.flixelgdx.tween.settings;

import org.flixelgdx.collections.FlixelArray;

import org.flixelgdx.functional.supplier.FloatSupplier;
import org.flixelgdx.tween.FlixelTween;
import org.flixelgdx.tween.FlixelTweenCallback;
import org.flixelgdx.tween.ease.FlixelEase;
import org.flixelgdx.tween.ease.FlixelEaseFunction;
import org.flixelgdx.tween.type.FlixelGoalTween;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Holds the configuration that drives how a {@link FlixelTween} animates.
 *
 * <p>Think of {@code FlixelTweenSettings} as the blueprint you hand to a tween before starting it.
 * It lets you define what values to change ({@link #addGoal}), how long the animation lasts
 * ({@link #setDuration}), the curve of the motion ({@link #setEase}), whether the tween loops or
 * ping-pongs ({@link #setType}), optional delays ({@link #setStartDelay}), and lifecycle callbacks
 * that fire when the tween starts, updates each frame, or finishes ({@link #setOnStart},
 * {@link #setOnUpdate}, {@link #setOnComplete}).
 *
 * <p>All setters return {@code this}, so you can chain them in a single expression. Example - moving
 * a sprite's x position from its current location to 400 over half a second with a smooth ease and
 * a completion log:
 *
 * <pre>{@code
 * FlixelTween.tween(sprite, new FlixelTweenSettings()
 *     .addGoal(sprite::getX, 400f, sprite::setX)
 *     .setDuration(0.5f)
 *     .setEase(FlixelEase::quadOut)
 *     .setOnComplete(tween -> Flixel.info("Slide done!")));
 * }</pre>
 *
 * <p>Goals are the properties the tween interpolates in a {@link FlixelGoalTween} via
 * {@link FlixelTween#tween(Object, FlixelTweenSettings)}. Each goal captures the starting value
 * once via a getter when the tween begins, then drives the value toward the target on every
 * frame via a setter. You can register multiple goals on a single tween to animate several
 * properties in lockstep, such as sliding a sprite both horizontally and vertically at once.
 *
 * @see FlixelTween
 * @see FlixelEase
 * @see FlixelTweenCallback
 * @see FlixelTweenType
 */
public class FlixelTweenSettings {

  private float duration;
  private float startDelay;
  private float loopDelay;
  private float framerate;
  private FlixelTweenType type;
  private FlixelEaseFunction ease;
  private FlixelTweenCallback onStart;
  private FlixelTweenCallback onUpdate;
  private FlixelTweenCallback onComplete;
  private final FlixelArray<FlixelTweenGoal> goals;

  /**
   * Constructs a new tween settings object with {@link FlixelTweenType#ONESHOT} as the default
   * type and {@link FlixelEase#linear(float)} for the default easer.
   */
  public FlixelTweenSettings() {
    this(FlixelTweenType.ONESHOT, FlixelEase::linear);
  }

  /**
   * Constructs a new tween settings object with {@link FlixelEase#linear(float)} as the
   * default easer.
   *
   * @param type The type of tween it should be.
   */
  public FlixelTweenSettings(@NotNull FlixelTweenType type) {
    this(type, FlixelEase::linear);
  }

  /**
   * Constructs a new tween settings object.
   *
   * @param type The type of tween it should be.
   * @param ease The easer function the tween should use (aka how it should be animated).
   */
  public FlixelTweenSettings(
      @NotNull FlixelTweenType type,
      @Nullable FlixelEaseFunction ease) {
    this.duration = 1.0f;
    this.startDelay = 0.0f;
    this.loopDelay = 0.0f;
    this.framerate = 0.0f;
    this.type = type;
    this.ease = ease;
    this.onStart = null;
    this.onUpdate = null;
    this.onComplete = null;
    this.goals = new FlixelArray<>(false, 16);
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
  public FlixelTweenSettings addGoal(
      @NotNull FlixelTweenGoal.FlixelTweenGoalGetter getter,
      float toValue,
      @NotNull FlixelTweenGoal.FlixelTweenGoalSetter setter) {
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

  public FlixelEaseFunction getEase() {
    return ease;
  }

  public FlixelTweenCallback getOnStart() {
    return onStart;
  }

  public FlixelTweenCallback getOnUpdate() {
    return onUpdate;
  }

  public FlixelTweenCallback getOnComplete() {
    return onComplete;
  }

  public FlixelArray<FlixelTweenGoal> getGoals() {
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

  /**
   * Sets the ease function used for this tween.
   *
   * @param ease The ease function to apply, or {@code null} for linear.
   * @return {@code this} tween settings object for chaining.
   */
  public FlixelTweenSettings setEase(FlixelEaseFunction ease) {
    this.ease = ease;
    return this;
  }

  /** Removes all tween goals added with {@link #addGoal}. */
  public void clearGoals() {
    goals.clear();
  }

  /**
   * Sets the delay before this tween begins playing for the first time.
   *
   * @param startDelay Delay in seconds before the first playback starts.
   * @return {@code this} tween settings object for chaining.
   */
  public FlixelTweenSettings setStartDelay(float startDelay) {
    this.startDelay = startDelay;
    return this;
  }

  /**
   * Sets the delay inserted between each loop iteration.
   *
   * @param loopDelay Delay in seconds between repeated plays.
   * @return {@code this} tween settings object for chaining.
   */
  public FlixelTweenSettings setLoopDelay(float loopDelay) {
    this.loopDelay = loopDelay;
    return this;
  }

  /**
   * Sets the target frame rate at which the tween updates its goals.
   *
   * <p>A value of {@code 0} means the tween updates every frame.
   *
   * @param framerate Target updates per second, or {@code 0} to update every frame.
   * @return {@code this} tween settings object for chaining.
   */
  public FlixelTweenSettings setFramerate(float framerate) {
    this.framerate = framerate;
    return this;
  }

  /**
   * Sets the tween type that controls looping behavior.
   *
   * @param type The new tween type.
   * @return {@code this} tween settings object for chaining.
   */
  public FlixelTweenSettings setType(@NotNull FlixelTweenType type) {
    this.type = type;
    return this;
  }

  /**
   * Sets a callback that fires once when this tween starts playing.
   *
   * @param onStart The callback, or {@code null} to clear it.
   * @return {@code this} tween settings object for chaining.
   */
  public FlixelTweenSettings setOnStart(FlixelTweenCallback onStart) {
    this.onStart = onStart;
    return this;
  }

  /**
   * Sets a callback that fires on every frame update while this tween is running.
   *
   * @param onUpdate The callback, or {@code null} to clear it.
   * @return {@code this} tween settings object for chaining.
   */
  public FlixelTweenSettings setOnUpdate(FlixelTweenCallback onUpdate) {
    this.onUpdate = onUpdate;
    return this;
  }

  /**
   * Sets a callback that fires once when this tween finishes playing.
   *
   * @param onComplete The callback, or {@code null} to clear it.
   * @return {@code this} tween settings object for chaining.
   */
  public FlixelTweenSettings setOnComplete(FlixelTweenCallback onComplete) {
    this.onComplete = onComplete;
    return this;
  }

  /**
   * Visits each registered goal with the given visitor.
   *
   * @param visitor The visitor to call for each goal.
   * @return {@code this} tween settings object for chaining.
   */
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
  public record FlixelTweenGoal(@NotNull FloatSupplier getter, float toValue,
      @NotNull FlixelTweenSettings.FlixelTweenGoal.FlixelTweenGoalSetter setter) {

    /** Supplies a primitive {@code float} without boxing. */
    @FunctionalInterface
    public interface FlixelTweenGoalGetter extends FloatSupplier {
      /** Returns the current property value as a primitive float. */
      float get();

      @Override
      default float getAsFloat() {
        return get();
      }
    }

    /** Consumes a primitive {@code float} without boxing. */
    @FunctionalInterface
    public interface FlixelTweenGoalSetter {
      /** Updates the property with the given interpolated float value. */
      void set(float value);
    }
  }

  /**
   * A functional interface for visiting each goal in {@code this} tween settings object.
   */
  @FunctionalInterface
  public interface FlixelTweenGoalVisitor {
    /** Called once for each goal registered in this tween settings object. */
    void visit(FlixelTweenGoal goal);
  }
}
