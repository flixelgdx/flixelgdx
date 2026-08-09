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
package org.flixelgdx.tween;

import org.flixelgdx.tween.settings.FlixelTweenSettings;

/**
 * A callback fired at a specific point in a tween's lifecycle.
 *
 * <p>The same interface is used for all three lifecycle hooks (start, update, and complete) so a
 * single lambda or method reference can be reused across hooks or shared between tweens without any
 * type-specific boilerplate.
 *
 * <p>Example - logging a message when a tween finishes:
 *
 * <pre>{@code
 * FlixelTweenCallback onDone = tween -> Flixel.info("Tween finished!");
 *
 * FlixelTween.tween(sprite, new FlixelTweenSettings()
 *     .addGoal(sprite::getX, 200f, sprite::setX)
 *     .setDuration(0.5f)
 *     .setOnStart(tween -> Flixel.info("Tween started!"))
 *     .setOnComplete(onDone));
 * }</pre>
 *
 * @see FlixelTweenSettings#setOnStart(FlixelTweenCallback)
 * @see FlixelTweenSettings#setOnUpdate(FlixelTweenCallback)
 * @see FlixelTweenSettings#setOnComplete(FlixelTweenCallback)
 */
@FunctionalInterface
public interface FlixelTweenCallback {

  /**
   * Called when the lifecycle event occurs.
   *
   * @param tween The tween that triggered this callback.
   */
  void run(FlixelTween tween);
}
