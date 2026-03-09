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
   * Remove an {@link FlixelTween} from {@code this} manager.
   * Note that when the FlixelTween is removed, it will call {@link FlixelTween#destroy} and it can no longer be used.
   *
   * @param tween The FlixelTween to remove.
   * @param destroy To determine if the FlixelTween will be destroyed upon calling the method.
   * @return  The removed FlixelTween object.
   */
  public FlixelTween removeTween(FlixelTween tween, boolean destroy) {
    if (tween == null) {
      return null;
    }

    tween.setActive(false);
    activeTweens.removeValue(tween, true);

    if (destroy) {
      tweenPool.free(tween);
    }

    return tween;
  }

  /**
   * Set {@link FlixelTween} for a restart.
   * {@link FlixelTween#restart} will be called on the next {@link FlixelTween#update}.
   *
   * @param tween The FlixelTween to set restart for.
   * @param waitingForRestart To determine if the FlixelTween will restart next time it is added to the active pool.
   */
  public void setRestartForTween(FlixelTween tween, boolean waitingForRestart) {
    tween.setWaitingForRestart(waitingForRestart);
  }

  /**
   * Add an {@link FlixelTween} to {@code this} manager.
   * When FlixelTween is added, it is added to the activeTweens SnapshortArray List.
   * If the FlixelTween is waiting for a restart, it will call {@link FlixelTween#restart}.
   *
   * @param tween The FlixelTween to add.
   */
  public void addTween (FlixelTween tween) {
    activeTweens.add(tween);
    if (tween.isWaitingForRestart()) {
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
