package me.stringdotjar.flixelgdx.tween;

import com.badlogic.gdx.utils.Pool;
import me.stringdotjar.flixelgdx.tween.settings.FlixelTweenSettings;
import me.stringdotjar.flixelgdx.tween.type.FlixelNumTween;
import me.stringdotjar.flixelgdx.tween.type.FlixelPropertyTween;
import me.stringdotjar.flixelgdx.tween.type.FlixelVarTween;
import org.jetbrains.annotations.NotNull;

/**
 * Core class for creating new tweens to add nice and smooth animations.
 *
 * <p>Please note that while this isn't an abstract class, it is advised to NOT create instances
 * of this class, since it does not implement the actual tweening logic. Instead, you should use one of
 * its subclasses, such as {@link FlixelVarTween}, or create your own subclass and add your own functionality.
 *
 * <p>The only reason this class is not abstract is to allow pooling of generic tweens when needed to save memory.
 */
public class FlixelTween implements Pool.Poolable {

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

  /** Is {@code this} tween currently active? */
  public boolean running = false;

  /** Is {@code this} tween finished tweening? */
  public boolean finished = false;

  /** Is {@code this} tween tweening backwards? */
  protected boolean backward = false;

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
   * Creates a new reflection-based tween with the provided settings and starts it in the global tween manager.
   *
   * @param object The object to tween its values.
   * @param tweenSettings The settings that configure and determine how the tween should animate.
   * @param updateCallback Callback function for updating the objects values when the tween updates.
   * @return The newly created and started tween.
   */
  public static FlixelTween tween(Object object, FlixelTweenSettings tweenSettings, FlixelVarTween.FunkinVarTweenUpdateCallback updateCallback) {
    return new FlixelVarTween(object, tweenSettings, updateCallback)
      .setManager(globalManager)
      .start();
  }

  /**
   * Creates a new property-based tween with the provided settings and starts it in the global tween manager.
   *
   * @param tweenSettings The settings that configure and determine how the tween should animate.
   * @return The newly created and started tween.
   */
  public static FlixelTween tween(FlixelTweenSettings tweenSettings) {
    return new FlixelPropertyTween(tweenSettings)
      .setManager(globalManager)
      .start();
  }

  /**
   * Creates a new numerical tween with the provided settings and starts it in the global tween manager.
   *
   * @param from The starting floating point value.
   * @param to The ending floating point value.
   * @param tweenSettings The settings that configure and determine how the tween should animate.
   * @param updateCallback Callback function for updating any variable(s) that needs the current value when the tween updates.
   * @return The newly created and started tween.
   */
  public static FlixelTween num(float from, float to, FlixelTweenSettings tweenSettings, FlixelNumTween.FlixelNumTweenUpdateCallback updateCallback) {
    return new FlixelNumTween(from, to, tweenSettings, updateCallback)
      .setManager(globalManager)
      .start();
  }

  /**
   * Starts {@code this} tween and resets every value to its initial state.
   *
   * @return {@code this} tween.
   */
  public FlixelTween start() {
    resetBasic();
    running = true;
    finished = false;
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
    if (paused || finished || !running || manager == null) {
      return;
    }
    if (tweenSettings == null) {
      return;
    }

    var ease = tweenSettings.getEase();
    var duration = tweenSettings.getDuration();
    var onStart = tweenSettings.getOnStart();
    var onUpdate = tweenSettings.getOnUpdate();
    var onComplete = tweenSettings.getOnComplete();
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
      scale = 1 - scale;
    }
    if (secondsSinceStart >= delay && !running) {
      running = true;
      if (onStart != null) {
        onStart.run(this);
      }
    }

    // Check if the tween has finished.
    if (secondsSinceStart >= duration + delay) {
      scale = (backward) ? 0 : 1;
      updateTweenValues();
      finished = true;
      if (onComplete != null) {
        onComplete.run(this);
      }
    } else {
      updateTweenValues();
      if (postTick > preTick && onUpdate != null) {
        onUpdate.run(this);
      }
    }
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
  protected void updateTweenValues() {
    // No-op by default; subclasses provide their own implementation.
  }

  /**
   * Resumes {@code this} tween if it was previously paused.
   *
   * @return {@code this} tween.
   */
  public FlixelTween resume() {
    paused = false;
    running = true;
    return this;
  }

  /**
   * Pauses {@code this} tween, stopping it from updating until resumed.
   *
   * @return {@code this} tween.
   */
  public FlixelTween pause() {
    paused = true;
    running = false;
    return this;
  }

  /**
   * Stops {@code this} tween. Note that this does not remove the tween from the active tweens in
   * its manager.
   *
   * @return {@code this} tween.
   */
  public FlixelTween stop() {
    running = false;
    return this;
  }

  /**
   * Restarts {@code this} tween if it is currently running and not finished.
   *
   * @return {@code this} tween.
   */
  public FlixelTween restart() {
    if (running && !finished && manager != null) {
      start();
    }
    return this;
  }

  /**
   * Cancels {@code this} tween, removes it from its manager and automatically defaults its values.
   */
  public FlixelTween cancel() {
    resetBasic();
    manager.getTweenPool().free(this);
    return this;
  }

  @Override
  public void reset() {
    resetBasic();
    manager = null;
  }

  /**
   * Resets only the basic values of {@code this} tween without removing any references to the
   * object, its settings or its callback function.
   */
  public void resetBasic() {
    scale = 0.0f;
    secondsSinceStart = 0.0f;
    executions = 0;
    paused = false;
    running = false;
    finished = false;
    backward = false;
  }

  public FlixelTweenSettings getTweenSettings() {
    return tweenSettings;
  }

  public FlixelTween setTweenSettings(@NotNull FlixelTweenSettings tweenSettings) {
    this.tweenSettings = tweenSettings;
    return this;
  }

  public static FlixelTweenManager getGlobalManager() {
    return globalManager;
  }

  public FlixelTweenManager getManager() {
    return manager;
  }

  public FlixelTween setManager(FlixelTweenManager newManager) {
    if (newManager != null) {
      if (manager != null) {
        int index = manager.getActiveTweens().indexOf(this, true);
        manager.getActiveTweens().removeIndex(index);
        manager.getTweenPool().free(this);
      }
      manager = newManager;
      manager.getActiveTweens().add(this);
    }
    return this;
  }
}
