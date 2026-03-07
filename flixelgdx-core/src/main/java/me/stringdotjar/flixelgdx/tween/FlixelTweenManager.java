package me.stringdotjar.flixelgdx.tween;

import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.SnapshotArray;

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
    FlixelTween[] tweens = activeTweens.begin();
    for (int i = 0, n = activeTweens.size; i < n; i++) {
      FlixelTween tween = tweens[i];
      if (tween == null) {
        continue;
      }
      tween.update(elapsed);

      if (tween.finished) {
        if (tween.manager != this) {
          continue;
        }
        var settings = tween.getTweenSettings();
        if (settings == null) {
          continue;
        }

        switch (settings.getType()) {
          case ONESHOT -> {
            activeTweens.removeValue(tween, true);
            tweenPool.free(tween);
          }
          case PERSIST -> {} // Do nothing, let it be.
        }
      }
    }
    activeTweens.end();
  }

  public SnapshotArray<FlixelTween> getActiveTweens() {
    return activeTweens;
  }

  public Pool<FlixelTween> getTweenPool() {
    return tweenPool;
  }
}
