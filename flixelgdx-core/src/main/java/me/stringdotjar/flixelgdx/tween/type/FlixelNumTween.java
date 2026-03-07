package me.stringdotjar.flixelgdx.tween.type;

import me.stringdotjar.flixelgdx.tween.FlixelTween;
import me.stringdotjar.flixelgdx.tween.settings.FlixelTweenSettings;

/**
 * Tween type that tweens one numerical value to another.
 */
public class FlixelNumTween extends FlixelTween {

  /** The starting value of the tween. */
  protected float start;

  /** The target value of the tween. */
  protected float end;

  /** The current value of the tween. */
  protected float value;

  /** The range between the start and end values. */
  protected float range;

  /** Callback function for updating the value when the tween updates. */
  protected FlixelNumTweenUpdateCallback updateCallback;

  /**
   * Constructs a new numerical tween, which will tween a simple starting number to an ending value.
   *
   * @param start The starting value.
   * @param end The ending value.
   * @param settings The settings that configure and determine how the tween should animate.
   * @param updateCallback Callback function for updating any variable that needs the current value when the tween updates.
   */
  public FlixelNumTween(float start, float end, FlixelTweenSettings settings, FlixelNumTweenUpdateCallback updateCallback) {
    super(settings);
    this.start = start;
    this.end = end;
    this.value = start;
    this.range = end - start;
    this.updateCallback = updateCallback;
  }

  @Override
  protected void updateTweenValues() {
    if (updateCallback == null) {
      return;
    }

    value = start + range * scale;

    updateCallback.update(value);
  }

  /**
   * Functional interface for updating the numerical value when the tween updates. This is for updating any
   * variable that needs the current value of the tween.
   */
  @FunctionalInterface
  public interface FlixelNumTweenUpdateCallback {

    /**
     * A callback method that is called when the tween updates its value during its tweening (or animating) process.
     *
     * @param value The new current value of the numerical tween during the animation.
     */
    void update(float value);
  }
}
