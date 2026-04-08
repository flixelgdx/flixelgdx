/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.tween;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;

import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.SnapshotArray;

import me.stringdotjar.flixelgdx.tween.builders.FlixelAbstractTweenBuilder;
import me.stringdotjar.flixelgdx.tween.settings.FlixelTweenSettings;
import me.stringdotjar.flixelgdx.tween.settings.FlixelTweenType;

/**
 * Manager class for handling a list of active {@link FlixelTween}s.
 *
 * normally used via {@link FlixelTween#getGlobalManager()} or the static helpers on {@link FlixelTween} (e.g.
 * {@link FlixelTween#updateTweens}, {@link FlixelTween#registerTweenType}) rather than instantiating separately.
 * Adding a tween via {@link #addTween(FlixelTween)} automatically starts it.
 *
 * <p>Uses a registry: each tween type is registered with its builder class and a pool factory.
 * Only registered types can be used with {@link FlixelTween#tween(Class, Class)}. Call
 * {@link #clearPools()} when clearing state (e.g. on state switch) to release pooled instances.
 */
public class FlixelTweenManager {

  /**
   * Registry entry for a tween type. Contains the builder class (for type verification), a factory that
   * creates new builder instances without reflection, and the pool used for tween reuse.
   *
   * @param builderClass The builder class, used for assignability checks in {@link FlixelTween#tween(Class, Class)}.
   * @param builderFactory A no-arg supplier that creates a new builder instance.
   *   This avoids reflective {@code newInstance()} calls that fail on platforms without full reflection support (for example TeaVM/web).
   * @param pool The object pool for recycling tween instances.
   */
  public static record TweenTypeRegistration(Class<?> builderClass, Supplier<? extends FlixelAbstractTweenBuilder<?, ?>> builderFactory, Pool<FlixelTween> pool) {}

  /** Registry: tween type -> (builder class, pool). */
  private final Map<Class<? extends FlixelTween>, TweenTypeRegistration> registry = new HashMap<>();

  /** Array where all current active tweens are stored. */
  protected final SnapshotArray<FlixelTween> activeTweens = new SnapshotArray<>(FlixelTween[]::new);

  /**
   * Registers a tween type with its builder factory and a pool factory for creating new tween instances when the pool is empty.
   * Register all tween types (including custom ones) before using {@link FlixelTween#tween(Class, Class)}.
   *
   * @param tweenClass The tween class (e.g. {@link me.stringdotjar.flixelgdx.tween.type.FlixelPropertyTween}{@code .class}).
   * @param builderClass The corresponding builder class, used for type verification when {@link FlixelTween#tween(Class, Class)} is called.
   * @param builderFactory A no-arg supplier that creates a fresh builder instance. Using an explicit factory avoids
   *   reflective {@code newInstance()} calls that fail on platforms without full reflection support (for example TeaVM).
   * @param poolFactory A supplier that creates a new tween instance when the pool is empty.
   * @param <T> The tween type.
   * @return The same manager, for chaining.
   * @throws IllegalArgumentException if the tween type is already registered.
   */
  public <T extends FlixelTween> FlixelTweenManager registerTweenType(
      Class<T> tweenClass,
      Class<? extends FlixelAbstractTweenBuilder<T, ?>> builderClass,
      Supplier<? extends FlixelAbstractTweenBuilder<T, ?>> builderFactory,
      Supplier<T> poolFactory) {
    Pool<FlixelTween> pool = new Pool<FlixelTween>() {
      @Override
      protected FlixelTween newObject() {
        return poolFactory.get();
      }
    };
    if (registry.containsKey(tweenClass)) {
      throw new IllegalArgumentException("Tween type " + tweenClass.getName() + " is already registered.");
    }
    registry.put(tweenClass, new TweenTypeRegistration(builderClass, builderFactory, pool));
    return this;
  }

  /**
   * Returns the builder class registered for the given tween type.
   *
   * @param tweenClass The registered tween class to look up.
   * @return The registered builder class.
   * @throws IllegalArgumentException if the tween type is not registered.
   */
  public Class<?> getBuilderClass(Class<? extends FlixelTween> tweenClass) {
    return getRegistration(tweenClass).builderClass();
  }

  /**
   * Creates a new builder instance for the given tween type using the registered builder factory.
   * This avoids reflective {@code getDeclaredConstructor().newInstance()} calls that are incompatible with
   * platforms lacking full reflection support (for example TeaVM/web).
   *
   * @param tweenType The tween class whose builder should be created.
   * @param <T> The tween type.
   * @param <B> The builder type.
   * @return A new builder instance, cast to the requested builder type.
   * @throws IllegalArgumentException if the tween type is not registered.
   */
  @SuppressWarnings("unchecked")
  public <T extends FlixelTween, B extends FlixelAbstractTweenBuilder<T, B>> B createBuilder(Class<T> tweenType) {
    return (B) getRegistration(tweenType).builderFactory().get();
  }

  /**
   * Adds the tween to this manager and starts it immediately.
   *
   * @param tween The tween to add and start.
   * @return The same tween, for chaining.
   */
  public FlixelTween addTween(FlixelTween tween) {
    if (tween == null) {
      return null;
    }

    activeTweens.add(tween);
    tween.manager = this;
    return tween.start();
  }

  /**
   * Updates all active tweens that are stored and updated in {@code this} manager.
   *
   * <p>Iterates in reverse so that finished ONESHOT tweens can be removed by index
   * without skipping elements or traversing null padding beyond the array's valid size.
   *
   * @param elapsed The amount of time that has passed since the last frame.
   */
  public void update(float elapsed) {
    FlixelTween[] items = activeTweens.begin();
    for (int i = 0; i < activeTweens.size; i++) {
      FlixelTween tween = items[i];
      if (tween == null || !tween.isActive()) {
        continue;
      }
      tween.update(elapsed);
    }

    for (int i = 0; i < activeTweens.size; i++) {
      FlixelTween tween = items[i];
      if (tween != null && tween.isFinished()) {
        if (tween.manager != this) {
          continue;
        }
        tween.finish();
      }
    }

    activeTweens.end();
  }

  /**
   * Obtains a tween of the given type from the registry's pool. The returned instance is reset;
   * the caller must set {@link FlixelTween#setTweenSettings} and any type-specific state (for
   * example {@link me.stringdotjar.flixelgdx.tween.type.FlixelPropertyTween#setObject(Object)})
   * before {@link #addTween(FlixelTween)}. The {@code factory} is only used when the type is not
   * registered; registered types ignore the supplier and use the pool's {@code newObject()} method.
   *
   * @param type The tween class (e.g. {@link me.stringdotjar.flixelgdx.tween.type.FlixelPropertyTween}.class).
   * @param factory Fallback factory when the type is not registered or the pool is empty.
   * @return A reset tween of type {@code T}, either from the pool or from {@code factory}.
   */
  @SuppressWarnings("unchecked")
  public <T extends FlixelTween> T obtainTween(Class<T> type, Supplier<T> factory) {
    return (T) getPool(type).obtain();
  }

  /**
   * Remove an {@link FlixelTween} from {@code this} manager.
   * When {@code destroy} is true, the tween is reset and returned to its type's pool if registered.
   *
   * @param tween The tween to remove.
   * @param destroy If true, reset the tween and free it to the pool (if its type is registered).
   * @return The removed tween.
   */
  public FlixelTween removeTween(FlixelTween tween, boolean destroy) {
    if (tween == null) {
      return null;
    }

    tween.setActive(false);
    activeTweens.removeValue(tween, true);

    if (destroy) {
      getPool(tween.getClass()).free(tween);
    }

    return tween;
  }

  public Pool<FlixelTween> getPool(Class<? extends FlixelTween> tweenClass) {
    return getRegistration(tweenClass).pool();
  }

  public void clearPools() {
    for (TweenTypeRegistration reg : registry.values()) {
      reg.pool().clear();
    }
  }

  public SnapshotArray<FlixelTween> getActiveTweens() {
    return activeTweens;
  }

  /**
   * Cancels all active tweens matching {@code object} and optional field paths (OR semantics).
   * When {@code fieldPaths} is empty, matches any tween {@link FlixelTween#isTweenOf(Object, String)} on {@code object} with a null/empty field.
   *
   * @param object Non-null root instance (same as passed to {@link FlixelTween#isTweenOf(Object, String)}).
   * @param fieldPaths Optional goal keys or dotted paths; empty means match all fields on {@code object}.
   */
  public void cancelTweensOf(Object object, String... fieldPaths) {
    if (object == null) {
      throw new IllegalArgumentException("Object to cancel tweens of cannot be null");
    }
    FlixelTween[] items = activeTweens.begin();
    try {
      for (int i = activeTweens.size - 1; i >= 0; i--) {
        FlixelTween tween = items[i];
        if (tween == null || !tween.isActive()) {
          continue;
        }
        if (matchesTweenOf(tween, object, fieldPaths)) {
          tween.cancel();
        }
      }
    } finally {
      activeTweens.end();
    }
  }

  /**
   * Completes matching tweens in one step (large delta). Non-{@link FlixelTweenType#isLooping() looping} tweens only.
   * {@link me.stringdotjar.flixelgdx.tween.settings.FlixelTweenSettings#getOnComplete()} runs when
   * {@link FlixelTween#finish()} is invoked after the tween reports finished.
   *
   * @param object The object to complete tweens of.
   * @param fieldPaths The field paths to complete tweens of.
   * @throws NullPointerException If the object is null.
   * @throws IllegalArgumentException If the object is null.
   */
  public void completeTweensOf(@NotNull Object object, String... fieldPaths) {
    if (object == null) {
      throw new IllegalArgumentException("Object to complete tweens of cannot be null");
    }
    // Iterate in reverse to avoid issues with ONESHOT tweens calling removeTween from finish(), which shrinks the list.
    // Forward iteration would skip the tween that shifted into the index we just advanced past (same pattern as cancelTweensOf).
    FlixelTween[] items = activeTweens.begin();
    try {
      for (int i = activeTweens.size - 1; i >= 0; i--) {
        FlixelTween tween = items[i];
        if (tween == null || !tween.isActive()) {
          continue;
        }
        if (!matchesTweenOf(tween, object, fieldPaths)) {
          continue;
        }
        FlixelTweenSettings settings = tween.getTweenSettings();
        if (settings != null && settings.getType().isLooping()) {
          continue;
        }
        tween.update(Float.MAX_VALUE);
        if (tween.isFinished()) {
          tween.finish();
        }
      }
    } finally {
      activeTweens.end();
    }
  }

  /**
   * Completes all active non-looping tweens.
   */
  public void completeAll() {
    FlixelTween[] items = activeTweens.begin();
    try {
      for (int i = activeTweens.size - 1; i >= 0; i--) {
        FlixelTween tween = items[i];
        if (tween == null || !tween.isActive()) {
          continue;
        }
        FlixelTweenSettings settings = tween.getTweenSettings();
        if (settings != null && settings.getType().isLooping()) {
          continue;
        }
        tween.update(Float.MAX_VALUE);
        if (tween.isFinished()) {
          tween.finish();
        }
      }
    } finally {
      activeTweens.end();
    }
  }

  /**
   * Completes all active tweens assignable to {@code type} (non-looping only).
   *
   * @param type The type of tween to complete.
   * @throws NullPointerException If the type is null.
   * @throws IllegalArgumentException If the type is null.
   */
  public void completeTweensOfType(Class<? extends FlixelTween> type) {
    if (type == null) {
      throw new IllegalArgumentException("Type to complete tweens of cannot be null");
    }
    FlixelTween[] items = activeTweens.begin();
    try {
      for (int i = activeTweens.size - 1; i >= 0; i--) {
        FlixelTween tween = items[i];
        if (tween == null || !tween.isActive() || !type.isInstance(tween)) {
          continue;
        }
        FlixelTweenSettings settings = tween.getTweenSettings();
        if (settings != null && settings.getType().isLooping()) {
          continue;
        }
        tween.update(Float.MAX_VALUE);
        if (tween.isFinished()) {
          tween.finish();
        }
      }
    } finally {
      activeTweens.end();
    }
  }

  /**
   * Returns whether any active tween matches {@code object} and optional {@code fieldPaths} (OR).
   *
   * @param object The object to check for tweens of.
   * @param fieldPaths The field paths to check for tweens of.
   * @throws NullPointerException If the object is null.
   * @throws IllegalArgumentException If the object is null.
   * @return True if the manager contains tweens of the given object and field paths, false otherwise.
   */
  public boolean containsTweensOf(Object object, String... fieldPaths) {
    if (object == null) {
      throw new IllegalArgumentException("Object to check for tweens of cannot be null");
    }
    FlixelTween[] items = activeTweens.begin();
    try {
      for (int i = 0; i < activeTweens.size; i++) {
        FlixelTween tween = items[i];
        if (tween != null && tween.isActive() && matchesTweenOf(tween, object, fieldPaths)) {
          return true;
        }
      }
    } finally {
      activeTweens.end();
    }
    return false;
  }

  /**
   * Invokes {@code action} for each active tween (current snapshot order).
   *
   * @param action The action to invoke for each active tween.
   * @throws NullPointerException If the action is null.
   */
  public void forEach(Consumer<FlixelTween> action) {
    if (action == null) {
      throw new IllegalArgumentException("Action cannot be null");
    }
    // We use a try/finally to ensure the array is always ended, even if an exception is thrown.
    FlixelTween[] items = activeTweens.begin();
    try {
      for (int i = 0; i < activeTweens.size; i++) {
        FlixelTween tween = items[i];
        if (tween != null) {
          action.accept(tween);
        }
      }
    } finally {
      activeTweens.end();
    }
  }

  /**
   * Clears the registry of all registered tween types and their respective pools.
   *
   * <p>It is advised to <strong>only call this if you know what you are doing</strong>, as
   * this will include the default registered tween types. If you call this, you will need to
   * register the tween types again.
   */
  public void resetRegistry() {
    forEach(FlixelTween::cancel);
    clearPools();
    activeTweens.clear();
    registry.clear();
  }

  private static boolean matchesTweenOf(FlixelTween tween, Object object, String[] fieldPaths) {
    if (fieldPaths == null || fieldPaths.length == 0) {
      return tween.isTweenOf(object, null);
    }
    for (String path : fieldPaths) {
      if (path == null) {
        if (tween.isTweenOf(object, null)) {
          return true;
        }
      } else if (tween.isTweenOf(object, path)) {
        return true;
      }
    }
    return false;
  }

  private TweenTypeRegistration getRegistration(Class<? extends FlixelTween> tweenClass) {
    TweenTypeRegistration reg = registry.get(tweenClass);
    if (reg == null) {
      throw new IllegalArgumentException("Tween type \"" + tweenClass.getName() + "\" is not registered. "
          + "Register it with FlixelTween.registerTweenType(...) or FlixelTweenManager.registerTweenType(...).");
    }
    return reg;
  }
}
