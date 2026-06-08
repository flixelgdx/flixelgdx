/*
 * MIT License
 *
 * Copyright (c) 2026 stringdotjar
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.flixelgdx.tween.type;

import org.flixelgdx.tween.FlixelTween;
import org.flixelgdx.tween.settings.FlixelTweenSettings;

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
  protected UpdateCallback updateCallback;

  /**
   * Constructs a new numerical tween, which will tween a simple starting number to an ending value.
   *
   * @param start The starting value.
   * @param end The ending value.
   * @param settings The settings that configure and determine how the tween should animate.
   * @param updateCallback Callback function for updating any variable that needs the current value when the tween updates.
   */
  public FlixelNumTween(float start, float end, FlixelTweenSettings settings,
      UpdateCallback updateCallback) {
    super(settings);
    this.start = start;
    this.end = end;
    this.value = start;
    this.range = end - start;
    this.updateCallback = updateCallback;
  }

  /**
   * Reconfigures this tween for reuse (e.g. from pool). Call before {@link #start()}.
   *
   * @param from Start value.
   * @param to End value.
   * @param updateCallback Callback for each updated value.
   * @return this, for chaining.
   */
  public FlixelNumTween setTarget(float from, float to, UpdateCallback updateCallback) {
    this.start = from;
    this.end = to;
    this.value = from;
    this.range = to - from;
    this.updateCallback = updateCallback;
    return this;
  }

  @Override
  public boolean isTweenOf(Object object, String field) {
    return false;
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
  public interface UpdateCallback {

    /**
     * A callback method that is called when the tween updates its value during its tweening (or animating) process.
     *
     * @param value The new current value of the numerical tween during the animation.
     */
    void update(float value);
  }
}
