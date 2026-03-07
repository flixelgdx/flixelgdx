package me.stringdotjar.flixelgdx.tween;

import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.SnapshotArray;

import java.util.ArrayList;

/** Manager class for handling a list of active {@link FlixelTween}s. */
public class FlixelTweenManager {

  /** Array where all current active tweens are stored. */
  protected final SnapshotArray<FlixelTween> activeTweens = new SnapshotArray<>(FlixelTween[]::new);

  /** A pool where all unused tweens are stored to preserve memory. */
  protected final Pool<FlixelTween> tweenPool = new Pool<>() {
    @Override
    protected FlixelTween newObject() {
      return new FlixelTween();
    }
  };

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
    ArrayList<FlixelTween> finishedTweens = new ArrayList<>();
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
        finishedTweens.add(tween);
      }
    }

    if(!finishedTweens.isEmpty()) {
      for(FlixelTween finishedTween : finishedTweens) {
        finishedTween.finish();
      }
    }
    activeTweens.end();
  }

  /**
   * Remove a FlixelTween
   *
   *
   * @param tween The FlixelTween to remove.
   * @param destroy Whether you want to destroy the FlixelTween.
   * @return	The removed FlixelTween object.
   */
  public FlixelTween removeTween(FlixelTween tween, Boolean destroy) {
    if (tween == null)
      return null;

    tween.active = false;
    activeTweens.removeValue(tween, true);

    if (destroy) {
      tween.destroy();
      tweenPool.free(tween);
    }

    return tween;
  }

  /**
   * Add FlixelTween to activeTweens array
   */
  public void addToActiveTweens(FlixelTween tween) {
    activeTweens.add(tween);
    if(tween.isWaitingForRestart()) {
      tween.restart();
    }
  }

  public SnapshotArray<FlixelTween> getActiveTweens() {
    return activeTweens;
  }

  public Pool<FlixelTween> getTweenPool() {
    return tweenPool;
  }
}
