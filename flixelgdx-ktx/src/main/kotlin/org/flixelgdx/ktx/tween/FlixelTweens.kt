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
@file:JvmName("FlixelTweens")

package org.flixelgdx.ktx.tween

import org.flixelgdx.tween.FlixelTween
import org.flixelgdx.tween.FlixelTweenCallback
import org.flixelgdx.tween.ease.FlixelEaseFunction
import org.flixelgdx.tween.settings.FlixelTweenSettings
import org.flixelgdx.tween.settings.FlixelTweenSettings.FlixelTweenGoal.FlixelTweenGoalGetter
import org.flixelgdx.tween.settings.FlixelTweenSettings.FlixelTweenGoal.FlixelTweenGoalSetter
import org.flixelgdx.tween.settings.FlixelTweenType

import java.util.function.Supplier

/**
 * A Kotlin builder DSL for [FlixelTween].
 *
 * The Java API drives a tween through a chained-setter [FlixelTweenSettings] object. This DSL
 * collapses those setters into named parameters and moves the property goals into a small builder
 * block, so a tween reads top to bottom:
 *
 * ```
 * sprite.tween(duration = 2f, ease = FlixelEase::quadOut, onComplete = { it.destroy() }) {
 *   goal({ sprite.x }, 100f) { sprite.x = it }
 *   goal({ sprite.y }, 0f) { sprite.y = it }
 * }
 * ```
 *
 * The getter and setter passed to [FlixelTweenGoalScope.goal] map straight to FlixelGDX's primitive
 * `float` goal interfaces, so they run without boxing while the tween updates every frame.
 */

/**
 * The receiver of a [tween] builder block, used to register property goals.
 *
 * @property settings The settings object the enclosing [tween] call is assembling.
 */
class FlixelTweenGoalScope(val settings: FlixelTweenSettings) {

  /**
   * Registers a goal that eases a single `float` property from its current value toward [to].
   *
   * @param getter Reads the property's starting value once, when the tween begins.
   * @param to The value to ease the property toward.
   * @param setter Receives the interpolated value on every update.
   */
  fun goal(getter: FlixelTweenGoalGetter, to: Float, setter: FlixelTweenGoalSetter) {
    settings.addGoal(getter, to, setter)
  }
}

/**
 * Creates and starts a tween on this object using an idiomatic Kotlin builder.
 *
 * Every setting has a sensible default, so only the goals block is required. See
 * [FlixelTweenGoalScope] for the goal syntax and a full example.
 *
 * @param duration How long the tween runs, in seconds.
 * @param ease The easing function, or `null` to keep the default linear ease.
 * @param type The tween type (one-shot, looping, ping-pong, and so on).
 * @param startDelay Seconds to wait before the tween begins.
 * @param loopDelay Seconds to wait between loops.
 * @param framerate The step rate for frame-based tweens, or `0` for smooth interpolation.
 * @param onStart Called when the tween starts, or `null` for none.
 * @param onUpdate Called on every update, or `null` for none.
 * @param onComplete Called when the tween finishes, or `null` for none.
 * @param goals Registers the property goals to ease; must add at least one.
 * @return The newly created and started tween.
 */
fun Any.tween(
  duration: Float = 1f,
  ease: FlixelEaseFunction? = null,
  type: FlixelTweenType = FlixelTweenType.ONESHOT,
  startDelay: Float = 0f,
  loopDelay: Float = 0f,
  framerate: Float = 0f,
  onStart: FlixelTweenCallback? = null,
  onUpdate: FlixelTweenCallback? = null,
  onComplete: FlixelTweenCallback? = null,
  goals: FlixelTweenGoalScope.() -> Unit,
): FlixelTween {
  val settings = FlixelTweenSettings(type)
  settings.duration = duration
  settings.startDelay = startDelay
  settings.loopDelay = loopDelay
  settings.framerate = framerate
  if (ease != null) {
    settings.ease = ease
  }
  if (onStart != null) {
    settings.onStart = onStart
  }
  if (onUpdate != null) {
    settings.onUpdate = onUpdate
  }
  if (onComplete != null) {
    settings.onComplete = onComplete
  }
  FlixelTweenGoalScope(settings).goals()
  return FlixelTween.tween(this, settings)
}

/**
 * Chains [next] to run after this tween finishes, so the callback reads as a trailing lambda.
 *
 * ```
 * sprite.tween(duration = 1f) { goal({ sprite.x }, 100f) { sprite.x = it } }
 *   .then { sprite.tween(duration = 1f) { goal({ sprite.x }, 0f) { sprite.x = it } } }
 * ```
 *
 * @param next Supplies the tween to start once this one completes.
 * @return This tween, for further chaining.
 */
inline fun FlixelTween.then(crossinline next: () -> FlixelTween): FlixelTween = then { next() }
