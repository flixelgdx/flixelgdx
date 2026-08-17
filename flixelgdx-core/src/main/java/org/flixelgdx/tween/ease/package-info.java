/**
 * Easing functions for controlling the rate of change in tween animations.
 *
 * <p>An easing function receives a normalized time value {@code t} in {@code [0, 1]} and returns
 * a shaped output value that the tweening engine uses to interpolate between a start and an end.
 * Choosing the right curve makes the difference between motion that feels mechanical and motion
 * that feels natural and responsive.
 *
 * <h2>Available functions</h2>
 * <p>All built-in functions live in {@link org.flixelgdx.tween.ease.FlixelEase FlixelEase}. Each
 * family comes in three variants:
 * <ul>
 *   <li>{@code *In} - slow start, fast finish</li>
 *   <li>{@code *Out} - fast start, slow finish (most commonly used for UI slides)</li>
 *   <li>{@code *InOut} - slow at both ends, fastest in the middle</li>
 * </ul>
 *
 * <p>The available families are:
 * <ul>
 *   <li><b>linear</b> - constant rate of change, no easing</li>
 *   <li><b>quad / cube / quart / quint</b> - polynomial curves from gentle to very sharp</li>
 *   <li><b>smoothStep / smootherStep</b> - Hermite-based smooth transitions</li>
 *   <li><b>sine / circ / expo</b> - trigonometric and exponential curves</li>
 *   <li><b>bounce</b> - simulates a ball bouncing off a surface</li>
 *   <li><b>back</b> - slight overshoot before settling at the target</li>
 *   <li><b>elastic</b> - spring-like oscillation around the target</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <p>Pass any function as a method reference to
 * {@link org.flixelgdx.tween.settings.FlixelTweenSettings#setEase(org.flixelgdx.tween.ease.FlixelEaseFunction)
 * FlixelTweenSettings.setEase(...)}:
 *
 * <pre>{@code
 * FlixelTween.tween(sprite, new FlixelTweenSettings()
 *     .addGoal(sprite::getX, 400f, sprite::setX)
 *     .setDuration(0.6f)
 *     .setEase(FlixelEase::bounceOut));
 * }</pre>
 *
 * <h2>Custom easing</h2>
 * <p>Implement {@link org.flixelgdx.tween.ease.FlixelEaseFunction FlixelEaseFunction} directly
 * to supply any curve you need:
 *
 * <pre>{@code
 * FlixelEaseFunction stepped = t -> (float) Math.floor(t * 4) / 4f;
 * }</pre>
 *
 * @see org.flixelgdx.tween.ease.FlixelEase
 * @see org.flixelgdx.tween.ease.FlixelEaseFunction
 * @see org.flixelgdx.tween.settings.FlixelTweenSettings
 */
package org.flixelgdx.tween.ease;
