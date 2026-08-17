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
package org.flixelgdx.tween.ease;

import org.flixelgdx.tween.FlixelTween;
import org.flixelgdx.tween.settings.FlixelTweenSettings;

/**
 * A single-method interface that maps a normalized time value to an eased output value.
 *
 * <p>The tweening system calls {@link #compute(float)} every frame with a progress value
 * {@code t} in {@code [0, 1]}, where {@code 0} is the start of the animation and {@code 1}
 * is the end. The return value controls how far along the animated property actually is at
 * that moment. Most easing functions also return values in {@code [0, 1]}, but functions such
 * as {@link FlixelEase#elasticOut(float)} and {@link FlixelEase#backOut(float)} temporarily
 * go outside that range to produce overshoot or oscillation effects.
 *
 * <p>Every built-in easing function in {@link FlixelEase} satisfies {@code f(0) = 0} and
 * {@code f(1) = 1}, so the animated property always starts and ends at the values you specify
 * regardless of the curve in between.
 *
 * <p>You typically supply an easing function to
 * {@link FlixelTweenSettings#setEase(FlixelEaseFunction)} using a method reference:
 *
 * <pre>{@code
 * FlixelTween.tween(sprite, new FlixelTweenSettings()
 *     .addGoal(sprite::getX, 400f, sprite::setX)
 *     .setDuration(0.8f)
 *     .setEase(FlixelEase::quadOut));
 * }</pre>
 *
 * <p>You can also write a custom easing function by implementing this interface directly:
 *
 * <pre>{@code
 * FlixelEaseFunction stepped = t -> (float) Math.floor(t * 4) / 4f;
 *
 * FlixelTween.tween(sprite, new FlixelTweenSettings()
 *     .addGoal(sprite::getX, 400f, sprite::setX)
 *     .setDuration(1f)
 *     .setEase(stepped));
 * }</pre>
 *
 * @see FlixelEase
 * @see FlixelTweenSettings#setEase(FlixelEaseFunction)
 * @see FlixelTween
 *
 * @author stringdotjar
 */
@FunctionalInterface
public interface FlixelEaseFunction {

  /**
   * Computes the eased output for the given normalized time.
   *
   * @param t Normalized time in the range {@code [0, 1]}, where {@code 0} is the animation start
   *     and {@code 1} is the end. Values outside this range may produce undefined results for most
   *     built-in functions.
   * @return The eased progress value. Most functions stay within {@code [0, 1]}, but elastic and
   *     back variants can exceed this range to produce overshoot effects.
   */
  float compute(float t);
}
