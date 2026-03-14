package me.stringdotjar.flixelgdx.tween;

import java.util.function.Supplier;

import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.SnapshotArray;

import me.stringdotjar.flixelgdx.tween.type.FlixelNumTween;
import me.stringdotjar.flixelgdx.tween.type.FlixelPropertyTween;
import me.stringdotjar.flixelgdx.tween.type.FlixelVarTween;

/**
 * Manager class for handling a list of active {@link FlixelTween}s.
 *
 * <p>Mirrors <a href="https://api.haxeflixel.com/flixel/tweens/FlxTweenManager.html">FlxTweenManager</a>:
 * normally used via {@link FlixelTween#getGlobalManager()} rather than instantiating separately.
 * Adding a tween via {@link #addTween(FlixelTween)} automatically starts it.
 *
 * <p>Uses a separate libGDX {@link Pool} per concrete tween type for reuse. Call {@link #clearPools()}
 * when clearing state (e.g. on state switch) to release pooled instances.
 */
public class FlixelTweenManager {

  /** Array where all current active tweens are stored. */
  protected final SnapshotArray<FlixelTween> activeTweens = new SnapshotArray<>(FlixelTween[]::new);

  private final Pool<FlixelPropertyTween> propertyTweenPool = new Pool<FlixelPropertyTween>() {
    @Override
    protected FlixelPropertyTween newObject() {
      return new FlixelPropertyTween(null);
    }
  };

  private final Pool<FlixelVarTween> varTweenPool = new Pool<FlixelVarTween>() {
    @Override
    protected FlixelVarTween newObject() {
      return new FlixelVarTween(null, null, null);
    }
  };

  private final Pool<FlixelNumTween> numTweenPool = new Pool<FlixelNumTween>() {
    @Override
    protected FlixelNumTween newObject() {
      return new FlixelNumTween(0, 0, null, null);
    }
  };

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
   * Obtains a tween of the given type from the appropriate pool, or creates one using the factory
   * if the type is not one of the built-in pooled types. The returned tween is reset; the caller
   * must set its settings (and any type-specific state) before adding it via {@link #addTween(FlixelTween)}.
   *
   * @param type The tween class (e.g. {@link FlixelPropertyTween}.class).
   * @param factory Creates a new tween when the type is not pooled or the pool is empty.
   * @return A reset tween of type {@code T}, either from the pool or from {@code factory}.
   */
  @SuppressWarnings("unchecked")
  public <T extends FlixelTween> T obtainTween(Class<T> type, Supplier<T> factory) {
    if (type == FlixelPropertyTween.class) {
      return (T) propertyTweenPool.obtain();
    }
    if (type == FlixelVarTween.class) {
      return (T) varTweenPool.obtain();
    }
    if (type == FlixelNumTween.class) {
      return (T) numTweenPool.obtain();
    }
    return factory.get();
  }

  /**
   * Remove an {@link FlixelTween} from {@code this} manager.
   * When {@code destroy} is true, the tween is reset and returned to its type-specific pool for reuse.
   *
   * @param tween The tween to remove.
   * @param destroy If true, reset the tween and free it to the pool (if it is a pooled type).
   * @return The removed tween.
   */
  public FlixelTween removeTween(FlixelTween tween, boolean destroy) {
    if (tween == null) {
      return null;
    }

    tween.setActive(false);
    activeTweens.removeValue(tween, true);

    if (destroy) {
      if (tween instanceof FlixelPropertyTween) {
        propertyTweenPool.free((FlixelPropertyTween) tween);
      } else if (tween instanceof FlixelVarTween) {
        varTweenPool.free((FlixelVarTween) tween);
      } else if (tween instanceof FlixelNumTween) {
        numTweenPool.free((FlixelNumTween) tween);
      }
      // Custom subclasses are not pooled; they are just dropped for GC
    }

    return tween;
  }

  public Pool<FlixelPropertyTween> getPropertyTweenPool() {
    return propertyTweenPool;
  }

  public Pool<FlixelVarTween> getVarTweenPool() {
    return varTweenPool;
  }

  public Pool<FlixelNumTween> getNumTweenPool() {
    return numTweenPool;
  }

  /**
   * Clears all tween pools. Call when switching state with {@code clearTweens} to release pooled instances.
   */
  public void clearPools() {
    propertyTweenPool.clear();
    varTweenPool.clear();
    numTweenPool.clear();
  }

  public SnapshotArray<FlixelTween> getActiveTweens() {
    return activeTweens;
  }
}
