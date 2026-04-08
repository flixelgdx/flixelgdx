/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.tween.type;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectFloatMap;
import com.badlogic.gdx.utils.ObjectMap;

import java.util.Objects;

import me.stringdotjar.flixelgdx.Flixel;
import me.stringdotjar.flixelgdx.backend.reflect.FlixelPropertyPath;
import me.stringdotjar.flixelgdx.tween.FlixelTween;
import me.stringdotjar.flixelgdx.tween.settings.FlixelTweenSettings;

/**
 * Tweens numeric properties on a target object by name using
 * {@link Flixel#reflect}.
 *
 * <p>At {@link #start()}, each goal value is read once with
 * {@link me.stringdotjar.flixelgdx.backend.reflect.FlixelReflection#property(Object, String)}
 * on the resolved leaf object (see
 * {@link me.stringdotjar.flixelgdx.backend.reflect.FlixelReflection#resolvePropertyPath(Object, String)}
 * for dotted paths such as {@code "child.x"}). On every update, interpolated values are written with
 * {@link me.stringdotjar.flixelgdx.backend.reflect.FlixelReflection#setProperty(Object, String, Object)},
 * so JavaBean setters run when present and behave like normal assignments.
 *
 * <p>Goals must resolve to a {@link Number} when read. Configure goals with {@link FlixelTweenSettings#addGoal(String, float)}. Install a
 * {@link me.stringdotjar.flixelgdx.backend.reflect.FlixelReflection} implementation via {@link Flixel#setReflection} before use; the default placeholder throws until then.
 *
 * <p>This tween type is slightly slower than {@link FlixelPropertyTween}, which avoids reflection by closing over getter/setter references.
 *
 * <h2>Web / TeaVM Compatibility Warning</h2>
 *
 * <p><strong>This tween type is not recommended for games targeting the web
 * (TeaVM) backend.</strong> It depends on runtime reflection to read and write
 * property values every frame. TeaVM compiles Java to JavaScript ahead of time
 * and supports reflection only through pre-generated metadata, which adds
 * overhead and can cause unexpected failures when metadata is incomplete.
 *
 * <p>If your game targets the web, prefer {@link FlixelPropertyTween} instead.
 * {@code FlixelPropertyTween} uses explicit getter/setter lambda references
 * and does not rely on reflection at runtime, making it fully compatible with
 * TeaVM and other ahead-of-time compilation targets.
 *
 * <p>To migrate, replace:
 * <pre>{@code
 * // VarTween (reflection-based, NOT recommended for web):
 * FlixelTween.tween(sprite, new FlixelTweenSettings()
 *     .addGoal("x", 100f)
 *     .setDuration(1f));
 *
 * // PropertyTween (lambda-based, recommended for web):
 * FlixelTween.tween(sprite, new FlixelTweenSettings()
 *     .addGoal(sprite::getX, 100f, sprite::setX)
 *     .setDuration(1f));
 * }</pre>
 *
 * @see FlixelPropertyTween
 * @see FlixelTweenSettings#addGoal(String, float)
 */
public class FlixelVarTween extends FlixelTween {

  /** The object to tween (root for path resolution). */
  protected Object object;

  /** Start values captured at {@link #start()} (or refreshed on manual {@link #restart()}). */
  protected final ObjectFloatMap<String> initialValues = new ObjectFloatMap<>();

  /** Goal key -> target value. */
  protected final ObjectFloatMap<String> goalValues = new ObjectFloatMap<>();

  /** Goal key -> leaf target and property name after resolving dotted paths. */
  protected final ObjectMap<String, FlixelPropertyPath> goalPaths = new ObjectMap<>();

  /**
   * Constructs a new var tween that will animate the given object's
   * properties using reflection. Goals must be added via
   * {@link FlixelTweenSettings#addGoal(String, float)} before starting.
   *
   * @param object The target object whose properties will be tweened.
   * @param settings The settings that configure how the tween animates.
   */
  public FlixelVarTween(Object object, FlixelTweenSettings settings) {
    super(settings);
    this.object = object;
  }

  /**
   * Sets the root target for property path resolution. Call this before
   * {@link #start()} when reusing a pooled tween whose target has changed.
   *
   * @param object The target object whose properties will be tweened.
   * @return {@code this} tween instance for method chaining.
   */
  public FlixelVarTween setObject(Object object) {
    this.object = object;
    return this;
  }

  @Override
  public FlixelTween start() {
    super.start();

    if (tweenSettings == null) {
      return this;
    }

    Array<FlixelTweenSettings.FlixelTweenVarGoal> goals = tweenSettings.getGoals();
    if (goals == null || goals.isEmpty()) {
      return this;
    }

    if (object == null) {
      throw new IllegalStateException(
          "FlixelVarTween requires a non-null target object before start(). "
              + "Call setTarget(object) or pass the object to FlixelTween.tween(object, settings).");
    }

    initialValues.clear();
    goalValues.clear();
    goalPaths.clear();

    tweenSettings.forEachGoal((fieldName, value) -> {
      FlixelPropertyPath path = Flixel.reflect.resolvePropertyPath(object, fieldName);
      Object raw = Flixel.reflect.property(path.leafObject(), path.leafName());
      initialValues.put(fieldName, requireNumeric(raw, fieldName));
      goalValues.put(fieldName, value);
      goalPaths.put(fieldName, path);
    });

    return this;
  }

  private static float requireNumeric(Object raw, String goalKey) {
    if (raw instanceof Number n) {
      return n.floatValue();
    }
    String kind = raw == null ? "null" : raw.getClass().getName();
    throw new IllegalStateException(
        "VarTween goal \"" + goalKey + "\" must be numeric when read via Flixel.reflect.property; got " + kind + ".");
  }

  @Override
  protected void updateTweenValues() {
    if (goalValues.isEmpty() || goalPaths.isEmpty()) {
      return;
    }
    for (ObjectFloatMap.Entry<String> entry : goalValues.entries()) {
      String key = entry.key;
      FlixelPropertyPath path = goalPaths.get(key);
      if (path == null) {
        continue;
      }
      float startValue = initialValues.get(key, 0f);
      float newValue = startValue + (entry.value - startValue) * scale;
      Flixel.reflect.setProperty(path.leafObject(), path.leafName(), newValue);
    }
  }

  @Override
  public void restart() {
    if (!internalRestart && tweenSettings != null && object != null && !goalPaths.isEmpty()) {
      Array<FlixelTweenSettings.FlixelTweenVarGoal> goals = tweenSettings.getGoals();
      if (goals != null && !goals.isEmpty()) {
        initialValues.clear();
        tweenSettings.forEachGoal((fieldName, value) -> {
          FlixelPropertyPath path = goalPaths.get(fieldName);
          if (path == null) {
            return;
          }
          Object raw = Flixel.reflect.property(path.leafObject(), path.leafName());
          initialValues.put(fieldName, requireNumeric(raw, fieldName));
        });
      }
    }
    super.restart();
  }

  @Override
  public void reset() {
    super.reset();
    goalPaths.clear();
    goalValues.clear();
    object = null;
  }

  @Override
  public void resetBasic() {
    super.resetBasic();
    initialValues.clear();
  }

  @Override
  public boolean isTweenOf(Object o, String field) {
    if (object == null || goalValues.isEmpty()) {
      return false;
    }
    if (field == null || field.isEmpty()) {
      return Objects.equals(o, object);
    }
    return Objects.equals(o, object) && goalValues.containsKey(field);
  }
}
