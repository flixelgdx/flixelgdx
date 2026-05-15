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

import me.stringdotjar.flixelgdx.tween.type.FlixelGoalTween;
import org.jetbrains.annotations.NotNull;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;

import me.stringdotjar.flixelgdx.tween.settings.FlixelTweenSettings;
import me.stringdotjar.flixelgdx.tween.settings.FlixelTweenType;

/**
 * Manager class for handling a list of active {@link FlixelTween}s.
 *
 * normally used via {@link FlixelTween#getGlobalManager()} or the static helpers on {@link FlixelTween} (e.g.
 * {@link FlixelTween#updateTweens}, {@link FlixelTween#registerTweenType}) rather than instantiating separately.
 * Adding a tween via {@link #addTween(FlixelTween)} automatically starts it.
 *
 * <p>Uses a registry: each tween type is registered with a pool factory. Registered types can be
 * obtained via {@link #obtainTween(Class, Supplier)}. Call
 * {@link #clearPools()} when clearing state (e.g. on state switch) to release pooled instances.
 *
 * <p>Active tweens use an unordered {@link Array}: removals swap with the last element (no
 * {@link com.badlogic.gdx.utils.SnapshotArray} copy-on-write), so the per-frame update path stays
 * allocation-free even when tweens finish and unregister.
 */
public class FlixelTweenManager {

  /**
   * Registry entry for a tween type: the object pool used for tween reuse.
   *
   * @param pool The object pool for recycling tween instances.
   */
  public record TweenTypeRegistration(Pool<FlixelTween> pool) {}

  /** Registry: tween class to its pool registration. */
  private final Map<Class<? extends FlixelTween>, TweenTypeRegistration> registry = new HashMap<>();

  /** Active tweens; unordered so {@link #removeTween} is O(1) without snapshot copies. */
  protected final Array<FlixelTween> activeTweens = new Array<>(false, 16, FlixelTween[]::new);

  /**
   * Registers a tween type with a pool factory for creating new tween instances when the pool is empty.
   * Register all tween types (including custom ones) before using {@link #obtainTween(Class, Supplier)} with
   * that tween class.
   *
   * @param tweenClass The tween class (e.g. {@link FlixelGoalTween}{@code .class}).
   * @param poolFactory A supplier that creates a new tween instance when the pool is empty.
   * @param <T> The tween type.
   * @return The same manager, for chaining.
   * @throws IllegalArgumentException if the tween type is already registered.
   */
  public <T extends FlixelTween> FlixelTweenManager registerTweenType(
      Class<T> tweenClass,
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
    registry.put(tweenClass, new TweenTypeRegistration(pool));
    return this;
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
   * <p>The finish pass runs in reverse order so {@link FlixelTween#finish()} can remove tweens
   * (swap-with-last removal) without skipping entries.
   *
   * @param elapsed The amount of time that has passed since the last frame.
   */
  public void update(float elapsed) {
    FlixelTween[] items = activeTweens.items;
    int n = activeTweens.size;
    for (int i = 0; i < n; i++) {
      FlixelTween tween = items[i];
      if (tween == null || !tween.isActive()) {
        continue;
      }
      tween.update(elapsed);
    }

    for (int i = activeTweens.size - 1; i >= 0; i--) {
      FlixelTween tween = activeTweens.items[i];
      if (tween != null && tween.isFinished()) {
        if (tween.manager != this) {
          continue;
        }
        tween.finish();
      }
    }
  }

  /**
   * Obtains a tween of the given type from the registry's pool. The returned instance is reset;
   * the caller must set {@link FlixelTween#setTweenSettings} and any type-specific state (for
   * example {@link FlixelGoalTween#setObject(Object)})
   * before {@link #addTween(FlixelTween)}. The {@code factory} is only used when the type is not
   * registered; registered types ignore the supplier and use the pool's {@code newObject()} method.
   *
   * @param type The tween class (e.g. {@link FlixelGoalTween}.class).
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

  public Array<FlixelTween> getActiveTweens() {
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
    for (int i = activeTweens.size - 1; i >= 0; i--) {
      FlixelTween tween = activeTweens.items[i];
      if (tween == null || !tween.isActive()) {
        continue;
      }
      if (matchesTweenOf(tween, object, fieldPaths)) {
        tween.cancel();
      }
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
    for (int i = activeTweens.size - 1; i >= 0; i--) {
      FlixelTween tween = activeTweens.items[i];
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
  }

  /**
   * Completes all active non-looping tweens.
   */
  public void completeAll() {
    for (int i = activeTweens.size - 1; i >= 0; i--) {
      FlixelTween tween = activeTweens.items[i];
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
    for (int i = activeTweens.size - 1; i >= 0; i--) {
      FlixelTween tween = activeTweens.items[i];
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
    for (int i = 0; i < activeTweens.size; i++) {
      FlixelTween tween = activeTweens.items[i];
      if (tween != null && tween.isActive() && matchesTweenOf(tween, object, fieldPaths)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Invokes {@code action} for each active tween. Iteration is from the end of the backing array
   * downward so {@code action} may cancel or remove tweens without skipping entries.
   *
   * @param action The action to invoke for each active tween.
   * @throws NullPointerException If the action is null.
   */
  public void forEach(Consumer<FlixelTween> action) {
    if (action == null) {
      throw new IllegalArgumentException("Action cannot be null");
    }
    for (int i = activeTweens.size - 1; i >= 0; i--) {
      FlixelTween tween = activeTweens.items[i];
      if (tween != null) {
        action.accept(tween);
      }
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
    for (int i = activeTweens.size - 1; i >= 0; i--) {
      FlixelTween t = activeTweens.items[i];
      if (t != null) {
        t.cancel();
      }
    }
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
