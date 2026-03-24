package me.stringdotjar.flixelgdx.tween;

import com.badlogic.gdx.utils.Pool;
import me.stringdotjar.flixelgdx.tween.builders.FlixelAbstractTweenBuilder;
import me.stringdotjar.flixelgdx.tween.builders.FlixelPropertyTweenBuilder;
import me.stringdotjar.flixelgdx.tween.settings.FlixelTweenSettings;
import me.stringdotjar.flixelgdx.tween.settings.FlixelTweenType;
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

  /** Is {@code this} tween active? */
  protected boolean active = false;

  /** Is {@code this} tween finished tweening? */
  public boolean finished = false;

  /** Is {@code this} tween tweening backwards? */
  protected boolean backward = false;

  /** Set during {@link #finish()} when restarting for LOOPING/PINGPONG so subclasses keep original start values. */
  protected boolean internalRestart = false;

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
   * Returns a fluent builder for the given tween type.
   *
   * <p>The tween type must be registered (e.g. via {@link FlixelTweenManager#registerTweenType}); if not, then
   * this method will throw an {@link IllegalArgumentException}.
   *
   * <p>Pass both the tween class and its builder class so the
   * return type is the concrete builder ({@code B}), giving full IDE support for type-specific methods
   * ({@code addGoal()}, {@code from()}, {@code to()}, etc.) and common ones ({@code setDuration},
   * {@code setEase}), then {@link FlixelAbstractTweenBuilder#start()}.
   *
   * <p>Example (property):
   * <pre>{@code
   * FlixelPropertyTween tween = FlixelTween.tween(FlixelPropertyTween.class, FlixelPropertyTweenBuilder.class)
   *   .addGoal(sprite::getX, 100f, sprite::setX)
   *   .setDuration(1f)
   *   .start();
   * }</pre>
   *
   * @param tweenType The tween class (e.g. {@link FlixelPropertyTween}.class). Must be registered.
   * @param builderType The corresponding builder class (e.g. {@link FlixelPropertyTweenBuilder}.class).
   * @return A new builder instance of type {@code B} for chaining.
   * @throws IllegalArgumentException If {@code tweenType} is not registered, or the registered builder could not be instantiated.
   */
  public static <T extends FlixelTween, B extends FlixelAbstractTweenBuilder<T, B>> B tween(Class<T> tweenType, Class<B> builderType) {
    Class<?> registeredBuilderClass = globalManager.getBuilderClass(tweenType);
    if (!builderType.isAssignableFrom(registeredBuilderClass)) {
      throw new IllegalArgumentException(
          "Registered builder for " + tweenType.getName() + " is " + registeredBuilderClass.getName()
              + ", which is not assignable to " + builderType.getName());
    }
    try {
      @SuppressWarnings("unchecked")
      B builder = (B) registeredBuilderClass.getDeclaredConstructor().newInstance();
      return builder;
    } catch (ReflectiveOperationException e) {
      throw new IllegalArgumentException("Could not instantiate builder " + registeredBuilderClass.getName() + ". It must have a no-arg constructor.", e);
    }
  }

  /**
   * Creates a new reflection-based tween with the provided settings and adds it to the global tween manager
   * (which starts it automatically). Shorthand for create, add and start, matching HaxeFlixel's FlxTween.tween.
   *
   * @param object The object to tween its values.
   * @param tweenSettings The settings that configure and determine how the tween should animate.
   * @param updateCallback Callback function for updating the objects values when the tween updates.
   * @return The newly created and started tween.
   */
  public static FlixelTween tween(Object object, FlixelTweenSettings tweenSettings, FlixelVarTween.FunkinVarTweenUpdateCallback updateCallback) {
    FlixelVarTween tween = globalManager.obtainTween(FlixelVarTween.class, () -> new FlixelVarTween(object, tweenSettings, updateCallback));
    return globalManager.addTween(tween);
  }

  /**
   * Creates a new property-based tween with the provided settings and adds it to the global tween manager
   * (which starts it automatically). Shorthand for create, add and start, matching HaxeFlixel's FlxTween.tween.
   *
   * @param tweenSettings The settings that configure and determine how the tween should animate.
   * @return The newly created and started tween.
   */
  public static FlixelTween tween(FlixelTweenSettings tweenSettings) {
    FlixelPropertyTween tween = globalManager.obtainTween(FlixelPropertyTween.class, () -> new FlixelPropertyTween(tweenSettings));
    return globalManager.addTween(tween);
  }

  /**
   * Creates a new numerical tween with the provided settings and adds it to the global tween manager
   * (which starts it automatically). Shorthand for create, add and start, matching HaxeFlixel's FlxTween.num.
   *
   * @param from The starting floating point value.
   * @param to The ending floating point value.
   * @param tweenSettings The settings that configure and determine how the tween should animate.
   * @param updateCallback Callback function for updating any variable(s) that needs the current value when the tween updates.
   * @return The newly created and started tween.
   */
  public static FlixelTween num(float from, float to, FlixelTweenSettings tweenSettings, FlixelNumTween.FlixelNumTweenUpdateCallback updateCallback) {
    FlixelNumTween tween = globalManager.obtainTween(FlixelNumTween.class, () -> new FlixelNumTween(from, to, tweenSettings, updateCallback));
    return globalManager.addTween(tween);
  }

  /**
   * Starts {@code this} tween and resets every value to its initial state.
   */
  public FlixelTween start() {
    if (tweenSettings != null && tweenSettings.getDuration() <= 0) {
      active = false;
      return this;
    }
    resetBasic();
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
    if (paused || !active || manager == null || tweenSettings == null) {
      return;
    }

    var ease = tweenSettings.getEase();
    var duration = tweenSettings.getDuration();
    var onStart = tweenSettings.getOnStart();
    var onUpdate = tweenSettings.getOnUpdate();
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
      scale = 1f - scale;
    }
    if (secondsSinceStart >= delay) {
      if (onStart != null) {
        onStart.run(this);
      }
    }
    // Check if the tween has finished.
    if (secondsSinceStart >= duration + delay) {
      scale = (backward) ? 0 : 1;
      updateTweenValues();
      finished = true;
    } else {
      updateTweenValues();
      if (postTick > preTick && onUpdate != null) {
        onUpdate.run(this);
      }
    }
  }

  /**
   * Called when the tween reaches the end of its duration. Invokes {@code onComplete} (including for LOOPING/PINGPONG each cycle).
   * LOOPING/PINGPONG restart (PINGPONG flips direction). Non-looping tweens (ONESHOT, PERSIST, BACKWARD) are deactivated so they stop updating and no longer overwrite the target; only ONESHOT is removed from the manager.
   */
  public void finish() {
    executions++;

    var onComplete = tweenSettings.getOnComplete();
    if (onComplete != null) {
      onComplete.run(this);
    }

    FlixelTweenType type = tweenSettings.getType();
    if (type.isLooping()) {
      secondsSinceStart = 0f;
      if (type == FlixelTweenType.PINGPONG) {
        backward = !backward;
      }
      internalRestart = true;
      restart();
      internalRestart = false;
    } else {
      active = false;
      if (type.removeOnFinish() && manager != null) {
        manager.removeTween(this, true);
      }
    }
  }

  /**
   * Sets {@code this} tween's {@link FlixelTweenSettings} and {@link FlixelTweenManager} to null.
   */
  public void destroy() {
    resetBasic();
    tweenSettings = null;
    manager = null;
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
    return this;
  }

  /**
   * Pauses {@code this} tween, stopping it from updating until resumed.
   *
   * @return {@code this} tween.
   */
  public FlixelTween pause() {
    paused = true;
    return this;
  }

  /**
   * Stops {@code this} tween. Note that this does not remove the tween from the active tweens in
   * its manager.
   *
   * @return {@code this} tween.
   */
  public FlixelTween stop() {
    active = false;
    return this;
  }

  /**
   * Restarts {@code this} tween from the beginning. Resets elapsed time and scale so the
   * tween runs again from the start (or from current property values for property/var
   * tweens when restarted manually).
   */
  public void restart() {
    if (tweenSettings == null || tweenSettings.getDuration() <= 0) {
      active = false;
      return;
    }
    secondsSinceStart = 0f;
    scale = 0f;
    active = true;
    finished = false;
  }

  /**
   * Cancels {@code this} tween, removes it from its manager and automatically defaults its values.
   */
  public FlixelTween cancel() {
    resetBasic();
    if (manager != null) {
      manager.removeTween(this, true);
    }
    return this;
  }

  @Override
  public void reset() {
    destroy();
  }

  /**
   * Resets only the basic values of {@code this} tween without removing any references to the
   * object, its settings or its callback function.
   */
  public void resetBasic() {
    scale = 0f;
    secondsSinceStart = 0f;
    executions = 0;
    paused = false;
    active = true;
    finished = false;
    backward = tweenSettings != null && tweenSettings.getType().isBackward();
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

  public boolean isFinished() {
    return finished;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public FlixelTween setManager(@NotNull FlixelTweenManager newManager) {
    if (manager != null) {
      manager.removeTween(this, true);
    }
    manager = newManager;
    manager.getActiveTweens().add(this);
    return this;
  }
}
