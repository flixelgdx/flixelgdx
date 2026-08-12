/**
 * Tweening system for interpolating values, positions, colors, and motion paths over time.
 *
 * <p>A tween animates a property from its current value to a target over a given duration,
 * optionally with an easing curve, a delay, repeats, and a completion callback. Tweens are
 * driven by a global {@link org.flixelgdx.tween.FlixelTweenManager FlixelTweenManager} held
 * inside {@link org.flixelgdx.tween.FlixelTween FlixelTween} and are automatically pooled: they
 * reset and return to the pool when they complete or are canceled.
 *
 * <h2>Common tween types</h2>
 * <ul>
 *   <li>Arbitrary object properties - {@link org.flixelgdx.tween.FlixelTween FlixelTween.tween(...)}</li>
 *   <li>Raw numeric values - {@link org.flixelgdx.tween.FlixelTween FlixelTween.num(...)}</li>
 *   <li>Color transitions - {@link org.flixelgdx.tween.FlixelTween FlixelTween.color(...)}</li>
 *   <li>Angle rotation - {@link org.flixelgdx.tween.FlixelTween FlixelTween.angle(...)}</li>
 *   <li>Screen shake - {@link org.flixelgdx.tween.FlixelTween FlixelTween.shake(...)}</li>
 *   <li>Visibility flicker - {@link org.flixelgdx.tween.FlixelTween FlixelTween.flicker(...)}</li>
 *   <li>Linear, quad, cubic, circular, and multipoint motion paths</li>
 * </ul>
 *
 * <h2>Basic example</h2>
 * <p>The simplest way to create a tween is through the static factory methods on
 * {@link org.flixelgdx.tween.FlixelTween FlixelTween}. Pass a
 * {@link org.flixelgdx.tween.settings.FlixelTweenSettings FlixelTweenSettings} to control
 * duration, easing, and callbacks:
 *
 * <pre>{@code
 * // Slide a sprite to x=300 over 1.5 seconds with a smooth ease-out:
 * FlixelTween.tween(sprite, new FlixelTweenSettings()
 *     .addGoal(sprite::getX, 300f, sprite::setX)
 *     .setDuration(1.5f)
 *     .setEase(FlixelEase::quadOut)
 *     .onComplete(t -> Flixel.info("Slide done!")));
 * }</pre>
 *
 * <h2>Easing curves</h2>
 * <p>All built-in easing functions are static references on
 * {@link org.flixelgdx.tween.ease.FlixelEase FlixelEase}. Each comes in {@code *In},
 * {@code *Out}, and {@code *InOut} variants. Pass one to
 * {@link org.flixelgdx.tween.settings.FlixelTweenSettings#setEase(org.flixelgdx.tween.ease.FlixelEaseFunction) FlixelTweenSettings.setEase(...)}
 * to control the rate of change:
 *
 * <pre>{@code
 * new FlixelTweenSettings()
 *     .setDuration(0.5f)
 *     .setEase(FlixelEase::bounceOut)
 * }</pre>
 *
 * <h2>Looping and ping-pong</h2>
 * <p>Control repetition and direction through
 * {@link org.flixelgdx.tween.settings.FlixelTweenSettings FlixelTweenSettings} and
 * {@link org.flixelgdx.tween.settings.FlixelTweenType FlixelTweenType}:
 *
 * <pre>{@code
 * new FlixelTweenSettings()
 *     .setDuration(1f)
 *     .setType(FlixelTweenType.PINGPONG) // Reverse on each repeat.
 * }</pre>
 *
 * @see org.flixelgdx.tween.FlixelTween
 * @see org.flixelgdx.tween.FlixelTweenManager
 * @see org.flixelgdx.tween.ease.FlixelEase
 * @see org.flixelgdx.tween.settings.FlixelTweenSettings
 */
package org.flixelgdx.tween;
