/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.tween;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;

import me.stringdotjar.flixelgdx.functional.FlixelAngleable;
import me.stringdotjar.flixelgdx.functional.FlixelColorable;
import me.stringdotjar.flixelgdx.functional.FlixelPositional;
import me.stringdotjar.flixelgdx.functional.FlixelShakeable;
import me.stringdotjar.flixelgdx.functional.FlixelVisible;
import me.stringdotjar.flixelgdx.tween.settings.FlixelShakeUnit;
import me.stringdotjar.flixelgdx.tween.settings.FlixelTweenSettings;
import me.stringdotjar.flixelgdx.tween.settings.FlixelTweenType;
import me.stringdotjar.flixelgdx.tween.type.FlixelAngleTween;
import me.stringdotjar.flixelgdx.tween.type.FlixelColorTween;
import me.stringdotjar.flixelgdx.tween.type.FlixelFlickerTween;
import me.stringdotjar.flixelgdx.tween.type.FlixelGoalTween;
import me.stringdotjar.flixelgdx.tween.type.FlixelNumTween;
import me.stringdotjar.flixelgdx.tween.type.FlixelShakeTween;
import me.stringdotjar.flixelgdx.tween.type.motion.FlixelCircularMotion;
import me.stringdotjar.flixelgdx.tween.type.motion.FlixelCubicMotion;
import me.stringdotjar.flixelgdx.tween.type.motion.FlixelLinearMotion;
import me.stringdotjar.flixelgdx.tween.type.motion.FlixelLinearPath;
import me.stringdotjar.flixelgdx.tween.type.motion.FlixelQuadMotion;
import me.stringdotjar.flixelgdx.tween.type.motion.FlixelQuadPath;
import me.stringdotjar.flixelgdx.util.FlixelAxes;
import me.stringdotjar.flixelgdx.util.FlixelColor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Base class for all FlixelGDX tweens and motion interpolators.
 * <p>
 * A {@code FlixelTween} provides a flexible system for animating properties, variables, or
 * custom behaviors over time, often used for smooth transitions, UI animations, procedural
 * effects, and advanced gameplay motions. FlixelTweens can manipulate values such as position,
 * color, angle, scale, or arbitrary numbers, and support features like easing, delay, repeats,
 * ping-pong (reverse-on-repeat), pausing, and callbacks for completion events.
 *
 * <h2>Common Use Cases</h2>
 * <ul>
 *   <li>Smoothly moving sprites or objects from one position to another</li>
 *   <li>Animating color transitions (e.g., fading to black, flashing effects)</li>
 *   <li>Pulsing, scaling, or rotating objects for visual effects</li>
 *   <li>Chaining tweens to create sequenced or looping animations</li>
 *   <li>Triggering sound or code when an animation completes</li>
 * </ul>
 *
 * <h2>How Tweens are Managed</h2>
 * <p>
 * Tweens are not updated automatically unless added to a {@link FlixelTweenManager}.
 * By default, all tweens are managed by a single global manager, but you can create and
 * control your own managers for local tween control (such as for a specific state or
 * menu screen).
 * </p>
 * <p>
 * Each tween may be paused, resumed, finished (immediately), restarted (for repeat behavior),
 * or canceled. Tweens are generally pooled for efficient memory use.
 * </p>
 *
 * <h2>Extension and Implementation</h2>
 * <p>
 * Subclasses of {@code FlixelTween} implement specialized behavior:
 * <ul>
 *   <li>{@link FlixelGoalTween}: interpolates properties of objects using lambda getters and setters.</li>
 *   <li>{@link me.stringdotjar.flixelgdx.tween.type.FlixelNumTween}: tweens a simple numeric value via a callback.</li>
 *   <li>{@link me.stringdotjar.flixelgdx.tween.type.FlixelColorTween}: tweens between colors</li>
 *   <li>{@link me.stringdotjar.flixelgdx.tween.type.FlixelAngleTween}: smoothly rotates a value</li>
 *   <li>{@link me.stringdotjar.flixelgdx.tween.type.motion.FlixelLinearMotion} and others: for advanced motion paths</li>
 * </ul>
 *
 * <h2>Key Fields</h2>
 * <ul>
 *   <li>{@code tweenSettings}: the configuration parameters for this tween (duration, repeat, easing, etc.)</li>
 *   <li>{@code manager}: the manager that updates and contains this tween instance</li>
 *   <li>{@code paused}, {@code active}, {@code finished}: control and status flags</li>
 *   <li>{@code scale}: represents current tween progress, interpolated from 0 to 1</li>
 *   <li>{@code secondsSinceStart}, {@code executions}: time-tracking for tween progress and repeats</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Start a tween that moves a sprite's x value from 0 to 100 over 1 second.
 * FlixelTween tween = FlixelTween.tween(sprite, new FlixelTweenSettings()
 *   .addGoal(sprite::getX, 100f, sprite::setX)
 *   .setDuration(1f)
 *   .setEase(FlixelEase::cubicInOut)
 *   .setOnComplete(tween -> Flixel.info("Done!")));
 * }</pre>
 *
 * <h2>Lifecycle and Pooling</h2>
 * <p>
 * When a tween completes (naturally or by calling {@link #finish()}), it is automatically
 * released back to a pool unless flagged otherwise. Do not hold references to finished
 * or canceled tweens if using pooling, as they may be reused. If you must keep the reference of a tween,
 * consider taking a look at {@link FlixelTweenType#PERSIST}.
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Tweens and their managers are generally intended for use on the game thread only.
 * </p>
 *
 * @see FlixelTweenManager
 * @see me.stringdotjar.flixelgdx.tween.settings.FlixelTweenSettings
 *
 * @author stringdotjar
 */

public abstract class FlixelTween implements Pool.Poolable {

  /** The global tween manager for the entire game. */
  private static final FlixelTweenManager globalManager = new FlixelTweenManager();

  /** The settings used for how the tween is handled and calculated (aka how it looks and animates). */
  protected FlixelTweenSettings tweenSettings;

  /** The parent manager that {@code this} tween gets updated in. */
  protected FlixelTweenManager manager;

  /** How far the tween is tweening itself. This is what's used to actually tween the object! */
  protected float scale = 0.0f;

  /** How many seconds has elapsed since {@code this} tween started. */
  protected float secondsSinceStart = 0.0f;

  /** How many times {@code this} tween has updated. */
  protected int executions = 0;

  /** Is {@code this} tween currently paused? */
  public boolean paused = false;

  /** Is {@code this} tween active? */
  protected boolean active = false;

  /** Is {@code this} tween finished tweening? */
  public boolean finished = false;

  /** Is {@code this} tween tweening backwards? */
  protected boolean backward = false;

  /** Set during {@link #finish()} when restarting for LOOPING/PINGPONG so subclasses keep original start values. */
  protected boolean internalRestart = false;

  /** Default constructor for pooling purposes. */
  protected FlixelTween() {}

  /**
   * Constructs a new tween with the provided settings.
   *
   * @param tweenSettings The settings that configure and determine how the tween should animate.
   */
  protected FlixelTween(FlixelTweenSettings tweenSettings) {
    this.tweenSettings = tweenSettings;
  }

  /**
   * Creates a tween with the provided settings and adds it to the global tween manager (which starts it automatically).
   *
   * <p>Goals must be {@linkplain FlixelTweenSettings#addGoal property goals} (getter and setter references). The
   * legacy string-based var goals API has been removed from the framework; use explicit getter and setter pairs so
   * tweens stay compatible with ahead-of-time targets, or integrate a third-party reflection helper in your game
   * project if you still need name-driven access (for example the ReflectAOT Gradle plugin).
   *
   * @param object The logical tween subject for {@link FlixelGoalTween#setObject(Object)} and
   *     {@link FlixelGoalTween#isTweenOf(Object, String)}.
   * @param tweenSettings Settings including property goals.
   * @return The newly created and started tween.
   * @throws IllegalArgumentException If no property goals are present.
   */
  public static FlixelTween tween(Object object, FlixelTweenSettings tweenSettings) {
    Objects.requireNonNull(tweenSettings, "tweenSettings");
    Array<FlixelTweenSettings.FlixelTweenGoal> propGoals = tweenSettings.getPropertyGoals();
    if (propGoals == null || propGoals.size == 0) {
      throw new IllegalArgumentException(
          "FlixelTweenSettings requires at least one property goal from addGoal(getter, toValue, setter).");
    }

    FlixelGoalTween propTween = globalManager.obtainTween(FlixelGoalTween.class, () -> new FlixelGoalTween(tweenSettings));
    propTween.setTweenSettings(tweenSettings);
    propTween.setObject(object);
    return globalManager.addTween(propTween);
  }

  /**
   * Creates a new numerical tween with the provided settings and adds it to the global tween manager
   * (which starts it automatically). Shorthand for create, add and start.
   *
   * @param from The starting floating point value.
   * @param to The ending floating point value.
   * @param tweenSettings The settings that configure and determine how the tween should animate.
   * @param updateCallback Callback function for updating any variable(s) that needs the current value when the tween updates.
   * @return The newly created and started tween.
   */
  public static FlixelTween num(float from, float to, FlixelTweenSettings tweenSettings, FlixelNumTween.FlixelNumTweenUpdateCallback updateCallback) {
    FlixelNumTween tween = globalManager.obtainTween(FlixelNumTween.class, () -> new FlixelNumTween(from, to, tweenSettings, updateCallback));
    tween.setTweenSettings(tweenSettings);
    tween.setTarget(from, to, updateCallback);
    return globalManager.addTween(tween);
  }

  /**
   * Creates a new angle tween with the provided settings and adds it to the global tween manager.
   *
   * @param target The object to tween the angle of.
   * @param toAngle The ending angle (degrees).
   * @param tweenSettings The settings that configure and determine how the tween should animate.
   * @return The newly created and started tween.
   */
  public static FlixelTween angle(@Nullable FlixelAngleable target, float toAngle, FlixelTweenSettings tweenSettings) {
    return angle(target, Float.NaN, toAngle, tweenSettings);
  }

  /**
   * Creates a new angle tween with the provided settings and adds it to the global tween manager.
   *
   * @param target The object to tween the angle of.
   * @param fromAngle The starting angle (degrees).
   * @param toAngle The ending angle (degrees).
   * @param tweenSettings The settings that configure and determine how the tween should animate.
   * @return The newly created and started tween.
   */
  public static FlixelTween angle(@Nullable FlixelAngleable target, float fromAngle, float toAngle, FlixelTweenSettings tweenSettings) {
    FlixelAngleTween tween = globalManager.obtainTween(FlixelAngleTween.class, () -> new FlixelAngleTween(tweenSettings));
    tween.setTweenSettings(tweenSettings);
    tween.setAngles(target, fromAngle, toAngle);
    return globalManager.addTween(tween);
  }

  /**
   * Creates a new color tween with the provided settings and adds it to the global tween manager.
   *
   * <p>It's advised you use this method rather than directly changing the color of a sprite, as
   * {@link FlixelColorTween} will handle the color interpolation and apply it to the sprite smoothly, rather
   * than causing a flash or jump in color.
   *
   * @param colorable The tint target; often a {@link me.stringdotjar.flixelgdx.FlixelSprite}.
   * @param from The starting color.
   * @param to The ending color.
   * @param tweenSettings The settings that configure and determine how the tween should animate.
   * @return The newly created and started tween.
   */
  public static FlixelTween color(
      @Nullable FlixelColorable colorable,
      @Nullable FlixelColor from,
      @Nullable FlixelColor to,
      FlixelTweenSettings tweenSettings) {
    return color(colorable, from, to, tweenSettings, null);
  }

  /**
   * Creates a new color tween with the provided settings and adds it to the global tween manager.
   *
   * <p>It's advised you use this method rather than directly changing the color of a sprite, as
   * {@link FlixelColorTween} will handle the color interpolation and apply it to the sprite smoothly, rather
   * than causing a flash or jump in color.
   *
   * @param colorable The tint target; often a {@link me.stringdotjar.flixelgdx.FlixelSprite}.
   * @param from The starting color.
   * @param to The ending color.
   * @param tweenSettings The settings that configure and determine how the tween should animate.
   * @param onColor The callback to run when the tween is complete.
   * @return The newly created and started tween.
   */
  public static FlixelTween color(
      @Nullable FlixelColorable colorable,
      @Nullable FlixelColor from,
      @Nullable FlixelColor to,
      FlixelTweenSettings tweenSettings,
      @Nullable Runnable onColor) {
    FlixelColorTween tween = globalManager.obtainTween(FlixelColorTween.class, () -> new FlixelColorTween(tweenSettings));
    tween.setTweenSettings(tweenSettings);
    tween.setColorEndpoints(colorable, from, to, onColor);
    return globalManager.addTween(tween);
  }

  /**
   * Creates a new color tween using libGDX {@link Color} values with the provided settings and
   * adds it to the global tween manager.
   *
   * <p>It's advised you use this method rather than directly changing the color of a sprite, as
   * {@link FlixelColorTween} will handle the color interpolation and apply it to the sprite smoothly, rather
   * than causing a flash or jump in color.
   *
   * @param colorable The tint target; often a {@link me.stringdotjar.flixelgdx.FlixelSprite}.
   * @param from The starting color.
   * @param to The ending color.
   * @param tweenSettings The settings that configure and determine how the tween should animate.
   * @return The newly created and started tween.
   */
  public static FlixelTween color(
      @Nullable FlixelColorable colorable,
      @Nullable Color from,
      @Nullable Color to,
      FlixelTweenSettings tweenSettings) {
    return color(colorable, from, to, tweenSettings, null);
  }

  /**
   * Creates a new color tween using libGDX {@link Color} values with the provided settings and
   * adds it to the global tween manager.
   *
   * <p>It's advised you use this method rather than directly changing the color of a sprite, as
   * {@link FlixelColorTween} will handle the color interpolation and apply it to the sprite smoothly, rather
   * than causing a flash or jump in color.
   *
   * @param colorable The tint target; often a {@link me.stringdotjar.flixelgdx.FlixelSprite}.
   * @param from The starting color.
   * @param to The ending color.
   * @param tweenSettings The settings that configure and determine how the tween should animate.
   * @param onColor The callback to run when the tween is complete.
   * @return The newly created and started tween.
   */
  public static FlixelTween color(
      @Nullable FlixelColorable colorable,
      @Nullable Color from,
      @Nullable Color to,
      FlixelTweenSettings tweenSettings,
      @Nullable Runnable onColor) {
    FlixelColorTween tween = globalManager.obtainTween(FlixelColorTween.class, () -> new FlixelColorTween(tweenSettings));
    tween.setTweenSettings(tweenSettings);
    tween.setColorEndpointsRaw(colorable, from, to, onColor);
    return globalManager.addTween(tween);
  }

  /**
   * Creates a new color tween using mixture of libGDX's {@link Color} and FlixelGDX's {@link FlixelColor} values with
   * the provided settings and adds it to the global tween manager.
   *
   * <p>It's advised you use this method rather than directly changing the color of a sprite, as
   * {@link FlixelColorTween} will handle the color interpolation and apply it to the sprite smoothly, rather
   * than causing a flash or jump in color.
   *
   * @param colorable The tint target; often a {@link me.stringdotjar.flixelgdx.FlixelSprite}.
   * @param from The starting color.
   * @param to The ending color.
   * @param tweenSettings The settings that configure and determine how the tween should animate.
   * @return The newly created and started tween.
   */
  public static FlixelTween color(
    @Nullable FlixelColorable colorable,
    @Nullable FlixelColor from,
    @Nullable Color to,
    FlixelTweenSettings tweenSettings) {
    return color(colorable, from.getGdxColor(), to, tweenSettings, null);
  }

  /**
   * Creates a new color tween using mixture of libGDX's {@link Color} and FlixelGDX's {@link FlixelColor} values with
   * the provided settings and adds it to the global tween manager.
   *
   * <p>It's advised you use this method rather than directly changing the color of a sprite, as
   * {@link FlixelColorTween} will handle the color interpolation and apply it to the sprite smoothly, rather
   * than causing a flash or jump in color.
   *
   * @param colorable The tint target; often a {@link me.stringdotjar.flixelgdx.FlixelSprite}.
   * @param from The starting color.
   * @param to The ending color.
   * @param tweenSettings The settings that configure and determine how the tween should animate.
   * @return The newly created and started tween.
   */
  public static FlixelTween color(
    @Nullable FlixelColorable colorable,
    @Nullable Color from,
    @Nullable FlixelColor to,
    FlixelTweenSettings tweenSettings) {
    return color(colorable, from, to.getGdxColor(), tweenSettings, null);
  }

  /**
   * Creates a shake tween using defaults {@link FlixelShakeUnit#FRACTION} and
   * {@code fadeOut == false} (full strength each frame until the tween ends). Pooled instances
   * are reset to those defaults before configuration.
   *
   * @param shakeable The shake channel to jitter (sprite offset, object position, window, etc.).
   * @param axes Axes that receive position jitter.
   * @param intensity With {@link FlixelShakeUnit#FRACTION}, use a small value (for example 0.05f).
   * @param tweenSettings Duration, ease, and callbacks.
   * @return The new tween, already added to the global manager.
   */
  public static FlixelTween shake(
      @Nullable FlixelShakeable shakeable,
      FlixelAxes axes,
      float intensity,
      FlixelTweenSettings tweenSettings) {
    return shake(shakeable, axes, intensity, tweenSettings, FlixelShakeUnit.FRACTION, false);
  }

  /**
   * Creates a shake tween with explicit {@link FlixelShakeUnit} and fade-out taper.
   *
   * @param shakeable The shake channel to jitter.
   * @param axes Axes that receive position jitter.
   * @param intensity Interpretation depends on {@code shakeUnit}.
   * @param tweenSettings Duration, ease, and callbacks.
   * @param shakeUnit {@link FlixelShakeUnit#FRACTION} or {@link FlixelShakeUnit#PIXELS}.
   * @param fadeOut If true, amplitude is scaled by {@code (1 - scale)} toward the end of the tween.
   * @return The new tween, already added to the global manager.
   */
  public static FlixelTween shake(
      @Nullable FlixelShakeable shakeable,
      FlixelAxes axes,
      float intensity,
      FlixelTweenSettings tweenSettings,
      FlixelShakeUnit shakeUnit,
      boolean fadeOut) {
    FlixelShakeTween tween = globalManager.obtainTween(FlixelShakeTween.class, () -> new FlixelShakeTween(tweenSettings));
    tween.setTweenSettings(tweenSettings);
    tween.setShake(shakeable, axes, intensity);
    tween.setShakeUnit(shakeUnit);
    tween.setFadeOut(fadeOut);
    return globalManager.addTween(tween);
  }

  /**
   * Creates a new flicker tween with the provided settings and adds it to the global tween manager.
   *
   * @param visible The visibility target to flicker.
   * @param tweenSettings The settings that configure and determine how the tween should animate.
   * @return The newly created and started tween.
   */
  public static FlixelTween flicker(@Nullable FlixelVisible visible, FlixelTweenSettings tweenSettings) {
    return flicker(visible, 0.08f, 0.5f, true, tweenSettings, null);
  }

  /**
   * Creates a new flicker tween with the provided settings and adds it to the global tween manager.
   *
   * @param visible The visibility target to flicker.
   * @param period The period of the flicker.
   * @param ratio The ratio of the flicker.
   * @param endVisibility The visibility of the flicker at the end.
   * @param tweenSettings The settings that configure and determine how the tween should animate.
   * @param tweenFunction The function to use for the flicker.
   * @return The newly created and started tween.
   */
  public static FlixelTween flicker(
      @Nullable FlixelVisible visible,
      float period,
      float ratio,
      boolean endVisibility,
      FlixelTweenSettings tweenSettings,
      @Nullable Predicate<FlixelFlickerTween> tweenFunction) {
    FlixelFlickerTween tween = globalManager.obtainTween(FlixelFlickerTween.class, () -> new FlixelFlickerTween(tweenSettings));
    tween.setTweenSettings(tweenSettings);
    tween.setFlicker(visible, period, ratio, endVisibility, tweenFunction);
    return globalManager.addTween(tween);
  }

  /**
   * Creates a new linear motion tween with the provided settings and adds it to the global tween manager.
   *
   * @param target The target to move.
   * @param fromX The starting X position.
   * @param fromY The starting Y position.
   * @param toX The ending X position.
   * @param toY The ending Y position.
   * @param durationOrSpeed The duration or speed of the motion.
   * @param useDuration Whether to use the duration or speed.
   * @param tweenSettings The settings that configure and determine how the tween should animate.
   * @return The newly created and started tween.
   */
  public static FlixelTween linearMotion(
      @Nullable FlixelPositional target,
      float fromX,
      float fromY,
      float toX,
      float toY,
      float durationOrSpeed,
      boolean useDuration,
      FlixelTweenSettings tweenSettings) {
    FlixelLinearMotion tween = globalManager.obtainTween(FlixelLinearMotion.class, () -> new FlixelLinearMotion(tweenSettings));
    tween.setTweenSettings(tweenSettings);
    tween.setMotion(fromX, fromY, toX, toY, durationOrSpeed, useDuration);
    tween.setMotionObject(target);
    return globalManager.addTween(tween);
  }

  /**
   * Creates a new circular motion tween with the provided settings and adds it to the global tween manager.
   *
   * @param target The target to move.
   * @param centerX The center X position.
   * @param centerY The center Y position.
   * @param radius The radius of the motion.
   * @param angleDeg The angle of the motion.
   * @param clockwise The direction of the motion.
   * @param durationOrSpeed The duration or speed of the motion.
   * @param useDuration Whether to use the duration or speed.
   * @param tweenSettings The settings that configure and determine how the tween should animate.
   * @return The newly created and started tween.
   */
  public static FlixelTween circularMotion(
      @Nullable FlixelPositional target,
      float centerX,
      float centerY,
      float radius,
      float angleDeg,
      boolean clockwise,
      float durationOrSpeed,
      boolean useDuration,
      FlixelTweenSettings tweenSettings) {
    FlixelCircularMotion tween = globalManager.obtainTween(FlixelCircularMotion.class, () -> new FlixelCircularMotion(tweenSettings));
    tween.setTweenSettings(tweenSettings);
    tween.setMotion(centerX, centerY, radius, angleDeg, clockwise, durationOrSpeed, useDuration);
    tween.setMotionObject(target);
    return globalManager.addTween(tween);
  }

  /**
   * Quadratic Bézier motion (one control point). Same timing rules as {@link #linearMotion}. If {@code useDuration} is
   * true, {@code durationOrSpeed} is seconds; otherwise pixels per second along the approximated curve length.
   *
   * @param target The target to move.
   * @param fromX The starting X position.
   * @param fromY The starting Y position.
   * @param cx The control point X position.
   * @param cy The control point Y position.
   * @param toX The ending X position.
   * @param toY The ending Y position.
   * @param durationOrSpeed The duration or speed of the motion.
   * @param useDuration Whether to use the duration or speed.
   */
  public static FlixelTween quadMotion(
      @Nullable FlixelPositional target,
      float fromX,
      float fromY,
      float cx,
      float cy,
      float toX,
      float toY,
      float durationOrSpeed,
      boolean useDuration,
      FlixelTweenSettings tweenSettings) {
    FlixelQuadMotion tween = globalManager.obtainTween(FlixelQuadMotion.class, () -> new FlixelQuadMotion(tweenSettings));
    tween.setTweenSettings(tweenSettings);
    tween.setMotion(fromX, fromY, cx, cy, toX, toY, durationOrSpeed, useDuration);
    tween.setMotionObject(target);
    return globalManager.addTween(tween);
  }

  /**
   * Cubic Bézier motion. {@code (p0x, p0y)} through {@code (p3x, p3y)} with control points {@code (p1x, p1y)} and
   * {@code (p2x,p2y)}.
   *
   * @param target The target to move.
   * @param p0x The starting X position.
   * @param p0y The starting Y position.
   * @param p1x The first control point X position.
   * @param p1y The first control point Y position.
   * @param p2x The second control point X position.
   * @param p2y The second control point Y position.
   * @param p3x The ending X position.
   * @param p3y The ending Y position.
   * @param durationOrSpeed The duration or speed of the motion.
   * @param useDuration Whether to use the duration or speed.
   * @param tweenSettings The settings that configure and determine how the tween should animate.
   * @return The newly created and started tween.
   */
  public static FlixelTween cubicMotion(
      @Nullable FlixelPositional target,
      float p0x,
      float p0y,
      float p1x,
      float p1y,
      float p2x,
      float p2y,
      float p3x,
      float p3y,
      float durationOrSpeed,
      boolean useDuration,
      FlixelTweenSettings tweenSettings) {
    FlixelCubicMotion tween = globalManager.obtainTween(FlixelCubicMotion.class, () -> new FlixelCubicMotion(tweenSettings));
    tween.setTweenSettings(tweenSettings);
    tween.setMotion(p0x, p0y, p1x, p1y, p2x, p2y, p3x, p3y, durationOrSpeed, useDuration);
    tween.setMotionObject(target);
    return globalManager.addTween(tween);
  }

  /**
   * Piecewise-linear path through vertices {@code x0,y0,x1,y1,...}. Requires at least two points (four floats).
   *
   * @param target The target to move.
   * @param durationOrSpeed The duration or speed of the motion.
   * @param useDuration Whether to use the duration or speed.
   * @param tweenSettings The settings that configure and determine how the tween should animate.
   * @param xy Alternating x and y coordinates along the path.
   * @return The newly created and started tween.
   */
  public static FlixelTween linearPath(
      @Nullable FlixelPositional target,
      float durationOrSpeed,
      boolean useDuration,
      FlixelTweenSettings tweenSettings,
      float... xy) {
    Objects.requireNonNull(tweenSettings, "tweenSettings");
    if (xy == null || xy.length < 4 || (xy.length & 1) != 0) {
      throw new IllegalArgumentException("linearPath requires an even number of floats with at least four values (two points).");
    }
    FlixelLinearPath tween = globalManager.obtainTween(FlixelLinearPath.class, () -> new FlixelLinearPath(tweenSettings));
    tween.setTweenSettings(tweenSettings);
    for (int i = 0; i < xy.length; i += 2) {
      tween.addPoint(xy[i], xy[i + 1]);
    }
    tween.setMotion(durationOrSpeed, useDuration);
    tween.setMotionObject(target);
    return globalManager.addTween(tween);
  }

  /**
   * Chain of quadratic Bézier segments. Points are {@code start, control, end, control, end, ...} (odd vertex count, at least
   * three points and six floats minimum).
   *
   * @param target The target to move.
   * @param durationOrSpeed The duration or speed of the motion.
   * @param useDuration Whether to use the duration or speed.
   * @param tweenSettings The settings that configure and determine how the tween should animate.
   * @param xy Alternating x and y for each vertex.
   * @return The newly created and started tween.
   */
  public static FlixelTween quadPath(
      @Nullable FlixelPositional target,
      float durationOrSpeed,
      boolean useDuration,
      FlixelTweenSettings tweenSettings,
      float... xy) {
    Objects.requireNonNull(tweenSettings, "tweenSettings");
    if (xy == null || xy.length < 6 || (xy.length & 1) != 0) {
      throw new IllegalArgumentException("quadPath requires an even number of floats with at least six values.");
    }
    int numPoints = xy.length / 2;
    if (numPoints < 3 || (numPoints & 1) == 0) {
      throw new IllegalArgumentException(
          "quadPath needs an odd number of vertices (at least 3): start, control, end, control, end, ...");
    }
    FlixelQuadPath tween = globalManager.obtainTween(FlixelQuadPath.class, () -> new FlixelQuadPath(tweenSettings));
    tween.setTweenSettings(tweenSettings);
    for (int i = 0; i < xy.length; i += 2) {
      tween.addPoint(xy[i], xy[i + 1]);
    }
    tween.setMotion(durationOrSpeed, useDuration);
    tween.setMotionObject(target);
    return globalManager.addTween(tween);
  }

  /**
   * Starts {@code this} tween and resets every value to its initial state.
   */
  public FlixelTween start() {
    if (tweenSettings != null && tweenSettings.getDuration() <= 0) {
      active = false;
      return this;
    }
    resetBasic();
    return this;
  }

  /**
   * Updates {@code this} tween by the given delta time.
   *
   * <p>If you wish to change how a tween's values are updated, then consider looking at
   * {@link FlixelTween#updateTweenValues}
   *
   * @param elapsed The amount of time that has passed since the last update.
   */
  public final void update(float elapsed) {
    if (paused || !active || manager == null || tweenSettings == null) {
      return;
    }

    var ease = tweenSettings.getEase();
    var duration = tweenSettings.getDuration();
    var onStart = tweenSettings.getOnStart();
    var onUpdate = tweenSettings.getOnUpdate();
    var framerate = tweenSettings.getFramerate();

    float preTick = secondsSinceStart;
    secondsSinceStart += elapsed;
    float postTick = secondsSinceStart;

    float delay = (executions > 0) ? tweenSettings.getLoopDelay() : tweenSettings.getStartDelay();
    if (secondsSinceStart < delay) {
      return;
    }

    if (framerate > 0) {
      preTick = Math.round(preTick * framerate) / framerate;
      postTick = Math.round(postTick * framerate) / framerate;
    }

    scale = Math.max((postTick - delay), 0.0f) / duration;
    if (ease != null) {
      scale = ease.compute(scale);
    }
    if (backward) {
      scale = 1f - scale;
    }
    if (secondsSinceStart >= delay) {
      if (onStart != null) {
        onStart.run(this);
      }
    }
    // Check if the tween has finished.
    if (secondsSinceStart >= duration + delay) {
      scale = (backward) ? 0 : 1;
      updateTweenValues();
      finished = true;
    } else {
      updateTweenValues();
      if (postTick > preTick && onUpdate != null) {
        onUpdate.run(this);
      }
    }
  }

  /**
   * Called when the tween reaches the end of its duration.
   *
   * <p>Invokes {@code onComplete} (including for LOOPING/PINGPONG each cycle). LOOPING/PINGPONG restart (PINGPONG flips direction).
   * Non-looping tweens (ONESHOT, PERSIST, BACKWARD) are deactivated so they stop updating and no longer overwrite the target; only
   * ONESHOT is removed from the manager.
   */
  public void finish() {
    executions++;

    var onComplete = tweenSettings.getOnComplete();
    if (onComplete != null) {
      onComplete.run(this);
    }

    FlixelTweenType type = tweenSettings.getType();
    if (type.isLooping()) {
      secondsSinceStart = 0f;
      if (type == FlixelTweenType.PINGPONG) {
        backward = !backward;
      }
      internalRestart = true;
      restart();
      internalRestart = false;
    } else {
      active = false;
      if (type.removeOnFinish() && manager != null) {
        manager.removeTween(this, true);
      }
    }
  }

  /**
   * Sets {@code this} tween's {@link FlixelTweenSettings} and {@link FlixelTweenManager} to null.
   */
  public void destroy() {
    resetBasic();
    tweenSettings = null;
    manager = null;
  }

  /**
   * Hook method called by {@link #update(float)} after {@link #scale} has been computed
   * and all common checks have passed. Subclasses should override this to apply their
   * tween-specific value updates instead of overriding {@link #update(float)}.
   *
   * <p>This method is guaranteed to only be called when the tween is active (not paused,
   * not finished, has a manager and settings). The {@link #scale} field is already set to
   * the correct value for the current frame.
   */
  protected abstract void updateTweenValues();

  /**
   * Resumes {@code this} tween if it was previously paused.
   *
   * @return {@code this} tween.
   */
  public FlixelTween resume() {
    paused = false;
    return this;
  }

  /**
   * Pauses {@code this} tween, stopping it from updating until resumed.
   *
   * @return {@code this} tween.
   */
  public FlixelTween pause() {
    paused = true;
    return this;
  }

  /**
   * Stops {@code this} tween. Note that this does not remove the tween from the active tweens in
   * its manager.
   *
   * @return {@code this} tween.
   */
  public FlixelTween stop() {
    active = false;
    return this;
  }

  /**
   * Restarts {@code this} tween from the beginning. Resets elapsed time and scale so the
   * tween runs again from the start (or from current property values for property/var
   * tweens when restarted manually).
   */
  public void restart() {
    if (tweenSettings == null || tweenSettings.getDuration() <= 0) {
      active = false;
      return;
    }
    secondsSinceStart = 0f;
    scale = 0f;
    active = true;
    finished = false;
  }

  /**
   * Cancels {@code this} tween, removes it from its manager and automatically defaults its values.
   */
  public FlixelTween cancel() {
    resetBasic();
    if (manager != null) {
      manager.removeTween(this, true);
    }
    return this;
  }

  @Override
  public void reset() {
    destroy();
  }

  /**
   * Resets only the basic values of {@code this} tween without removing any references to the
   * object, its settings or its callback function.
   */
  public void resetBasic() {
    scale = 0f;
    secondsSinceStart = 0f;
    executions = 0;
    paused = false;
    active = true;
    finished = false;
    backward = tweenSettings != null && tweenSettings.getType().isBackward();
  }

  public FlixelTweenSettings getTweenSettings() {
    return tweenSettings;
  }

  public FlixelTween setTweenSettings(@NotNull FlixelTweenSettings tweenSettings) {
    this.tweenSettings = tweenSettings;
    return this;
  }

  /**
   * Registers a tween type with a pool factory on the global manager.
   * Returns the manager so calls can be chained when registering several types at startup.
   *
   * @param tweenClass The tween class to register.
   * @param poolFactory A supplier for new tween instances when the pool is empty.
   * @param <T> The tween type.
   * @return The global manager, for chaining.
   * @throws NullPointerException If {@code poolFactory} is {@code null}.
   * @see FlixelTweenManager#registerTweenType(Class, Supplier)
   */
  public static <T extends FlixelTween> FlixelTweenManager registerTweenType(
      @NotNull Class<T> tweenClass,
      @NotNull Supplier<T> poolFactory) {
    Objects.requireNonNull(poolFactory, "poolFactory");
    return globalManager.registerTweenType(tweenClass, poolFactory);
  }

  /**
   * Advances every active tween on the global manager by {@code elapsed} seconds.
   *
   * @param elapsed The elapsed time in seconds.
   */
  public static void updateTweens(float elapsed) {
    globalManager.update(elapsed);
  }

  /**
   * Cancels active tweens whose {@link #isTweenOf(Object, String)} matches {@code object} and optional {@code fieldPaths}
   * (OR semantics). Empty {@code fieldPaths} matches any field on {@code object} for supporting tween types.
   *
   * @param object The object to cancel tweens of.
   * @param fieldPaths The field paths to cancel tweens of.
   * @throws NullPointerException If the object is null.
   *
   * @see FlixelTweenManager#cancelTweensOf(Object, String...)
   */
  public static void cancelTweensOf(@NotNull Object object, String... fieldPaths) {
    Objects.requireNonNull(object, "object");
    globalManager.cancelTweensOf(object, fieldPaths);
  }

  /**
   * Snaps matching non-looping tweens to their end state in one step (large delta) and runs completion logic where
   * applicable.
   *
   * @param object The object to complete tweens of.
   * @param fieldPaths The field paths to complete tweens of.
   * @throws NullPointerException If the object is null.
   *
   * @see FlixelTweenManager#completeTweensOf(Object, String...)
   */
  public static void completeTweensOf(@NotNull Object object, String... fieldPaths) {
    Objects.requireNonNull(object, "object");
    globalManager.completeTweensOf(object, fieldPaths);
  }

  /** Completes all active non-looping tweens on the global manager. */
  public static void completeAllTweens() {
    globalManager.completeAll();
  }

  /**
   * Completes active non-looping tweens assignable to {@code type}.
   *
   * @param type The type of tween to complete.
   * @throws NullPointerException If the type is null.
   *
   * @see FlixelTweenManager#completeTweensOfType(Class)
   */
  public static void completeTweensOfType(@NotNull Class<? extends FlixelTween> type) {
    Objects.requireNonNull(type, "type");
    globalManager.completeTweensOfType(type);
  }

  /**
   * Checks if the global manager contains tweens of the given object and field paths.
   *
   * @param object The object to check.
   * @param fieldPaths The field paths to check.
   * @throws NullPointerException If the object is null.
   * @return True if the global manager contains tweens of the given object and field paths, false otherwise.
   *
   * @see FlixelTweenManager#containsTweensOf(Object, String...)
   */
  public static boolean containsTweensOf(@NotNull Object object, String... fieldPaths) {
    Objects.requireNonNull(object, "object");
    return globalManager.containsTweensOf(object, fieldPaths);
  }

  /** Clears every tween pool on the global manager (e.g. after a major state reset). */
  public static void clearTweenPools() {
    globalManager.clearPools();
  }

  /**
   * Resets the registry of all registered tween types and their respective pools.
   *
   * <p>It is advised to <strong>only call this if you know what you are doing</strong>, as
   * this will include the default registered tween types. If you call this, you will need to
   * register the tween types again.
   */
  public static void resetRegistry() {
    globalManager.resetRegistry();
  }

  /**
   * Cancels every active tween on the global manager. Does not clear pools; pair with {@link #clearTweenPools()} if you
   * want a full reset (as {@link me.stringdotjar.flixelgdx.Flixel#switchState} does when {@code clearTweens} is true).
   */
  public static void cancelActiveTweens() {
    Array<FlixelTween> list = globalManager.getActiveTweens();
    for (int i = list.size - 1; i >= 0; i--) {
      FlixelTween t = list.get(i);
      if (t != null) {
        t.cancel();
      }
    }
  }

  public static FlixelTweenManager getGlobalManager() {
    return globalManager;
  }

  public FlixelTweenManager getManager() {
    return manager;
  }

  public boolean isFinished() {
    return finished;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public FlixelTween setManager(@NotNull FlixelTweenManager newManager) {
    if (manager != null) {
      manager.removeTween(this, true);
    }
    manager = newManager;
    manager.getActiveTweens().add(this);
    return this;
  }

  /**
   * Whether this tween is considered to animate {@code object} for the given logical field or path.
   * Used by {@link FlixelTweenManager#cancelTweensOf(Object, String...)} and related APIs.
   *
   * @param object The instance to test (e.g. the root passed to {@code cancelTweensOf}).
   * @param field Optional goal key or dotted path (e.g. {@code "x"} or {@code "weapon.rotation"}); {@code null} or
   *     empty matches any field on {@code object} for types that support it.
   * @return {@code false} by default; subclasses override.
   */
  public boolean isTweenOf(Object object, String field) {
    return false;
  }
}
