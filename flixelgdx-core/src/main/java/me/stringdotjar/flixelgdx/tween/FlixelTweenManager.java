/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.tween;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.SnapshotArray;

import me.stringdotjar.flixelgdx.tween.builders.FlixelAbstractTweenBuilder;

/**
 * Manager class for handling a list of active {@link FlixelTween}s.
 *
 * <p>Mirrors <a href="https://api.haxeflixel.com/flixel/tweens/FlxTweenManager.html">FlxTweenManager</a>:
 * normally used via {@link FlixelTween#getGlobalManager()} rather than instantiating separately.
 * Adding a tween via {@link #addTween(FlixelTween)} automatically starts it.
 *
 * <p>Uses a registry: each tween type is registered with its builder class and a pool factory.
 * Only registered types can be used with {@link FlixelTween#tween(Class, Class)}. Call
 * {@link #clearPools()} when clearing state (e.g. on state switch) to release pooled instances.
 */
public class FlixelTweenManager {

  /**
   * Registry entry for a tween type. Contains its builder class and the pool used for reuse.
   */
  public static record TweenTypeRegistration(Class<?> builderClass, Pool<FlixelTween> pool) {}

  /** Registry: tween type -> (builder class, pool). */
  private final Map<Class<? extends FlixelTween>, TweenTypeRegistration> registry = new HashMap<>();

  /** Array where all current active tweens are stored. */
  protected final SnapshotArray<FlixelTween> activeTweens = new SnapshotArray<>(FlixelTween[]::new);

  /**
   * Registers a tween type with its builder class and a factory for creating new instances when the pool is empty.
   * Register all tween types (including custom ones) before using {@link FlixelTween#tween(Class, Class)}.
   *
   * @param tweenClass The tween class (e.g. {@link me.stringdotjar.flixelgdx.tween.type.FlixelPropertyTween}.class).
   * @param builderClass The corresponding builder class (e.g. {@link me.stringdotjar.flixelgdx.tween.builders.FlixelPropertyTweenBuilder}.class).
   * @param poolFactory Supplies a new tween instance when the pool is empty (used for reset/poolable instances).
   * @param <T> The tween type.
   * @return this manager, for chaining.
   */
  public <T extends FlixelTween> FlixelTweenManager registerTweenType(
      Class<T> tweenClass,
      Class<? extends FlixelAbstractTweenBuilder<T, ?>> builderClass,
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
    registry.put(tweenClass, new TweenTypeRegistration(builderClass, pool));
    return this;
  }

  /**
   * Returns the builder class registered for the given tween type.
   *
   * @param tweenClass The registered tween class to look up.
   * @return The registered builder class.
   * @throws IllegalArgumentException If the registered tween type is not registered.
   */
  public Class<?> getBuilderClass(Class<? extends FlixelTween> tweenClass) {
    return getRegistration(tweenClass).builderClass();
  }

  /**
   * Adds the tween to this manager and starts it immediately.
   *
   * @param tween The tween to add and start.
   * @return The same tween for chaining.
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
   * Obtains a tween of the given type from the registry's pool, or creates one using the factory
   * if the type is not registered. The returned tween is reset; the caller must set its settings
   * (and any type-specific state) before adding it via {@link #addTween(FlixelTween)}.
   *
   * @param type The tween class (e.g. {@link me.stringdotjar.flixelgdx.tween.type.FlixelPropertyTween}.class).
   * @param factory Creates a new tween when the type is not registered or the pool is empty.
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

  private TweenTypeRegistration getRegistration(Class<? extends FlixelTween> tweenClass) {
    TweenTypeRegistration reg = registry.get(tweenClass);
    if (reg == null) {
      throw new IllegalArgumentException("Tween type \"" + tweenClass.getName() + "\" is not registered. "
          + "Register it with FlixelTweenManager.registerTweenType().");
    }
    return reg;
  }
}
