package me.stringdotjar.flixelgdx.tween;

import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.SnapshotArray;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import static me.stringdotjar.flixelgdx.tween.settings.FlixelTweenType.ONESHOT;
import static me.stringdotjar.flixelgdx.tween.settings.FlixelTweenType.PINGPONG;

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
    ArrayList<FlixelTween> finishedTweens = getFlixelTweens(elapsed, tweens);

    if(!finishedTweens.isEmpty()) {
      for(FlixelTween finishedTween : finishedTweens) {
        finishedTween.finish();
      }
    }
    activeTweens.end();
  }

  private ArrayList<FlixelTween> getFlixelTweens(float elapsed, FlixelTween[] tweens) {
    ArrayList<FlixelTween> finishedTweens = new ArrayList<>();
    for (int i = 0, n = activeTweens.size; i < n; i++) {
      FlixelTween tween = tweens[i];
      if (tween == null || !tween.isActive()) {
        continue;
      }
      tween.update(elapsed);

      if (tween.isFinished()) {
        if (tween.manager != this) {
          continue;
        }
        var settings = tween.getTweenSettings();
        if (settings == null) {
          continue;
        }

        finishedTweens.add(tween);
      }
    }
    return finishedTweens;
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

    if (destroy) {
      tween.destroy();
      tweenPool.free(tween);
    }

    activeTweens.removeValue(tween, true);

    return tween;
  }

  /**
   * Add FlixelTween to activeTweens array
   */
  public void addToActiveTweens(FlixelTween tween) {
    activeTweens.add(tween);
  }

  public SnapshotArray<FlixelTween> getActiveTweens() {
    return activeTweens;
  }

  public Pool<FlixelTween> getTweenPool() {
    return tweenPool;
  }
}
