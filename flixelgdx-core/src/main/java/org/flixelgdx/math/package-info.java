/**
 * Math value types and helpers owned by FlixelGDX.
 *
 * <p>This package holds the framework's math surface: the geometric value types
 * ({@link org.flixelgdx.math.FlixelVector FlixelVector},
 * {@link org.flixelgdx.math.FlixelRect FlixelRect}), the static math helpers in
 * {@link org.flixelgdx.math.FlixelMath FlixelMath}, the seedable random generator
 * {@link org.flixelgdx.math.FlixelRandom FlixelRandom}, and the transform types
 * {@link org.flixelgdx.math.FlixelMatrix FlixelMatrix} and
 * {@link org.flixelgdx.math.FlixelAffine FlixelAffine}. Together they cover the arithmetic,
 * geometry, and randomness game code needs without reaching outside the framework.
 *
 * <h2>Static helpers - FlixelMath</h2>
 * <p>All methods are allocation-free and safe to call inside update and render loops. Common
 * patterns:
 *
 * <pre>{@code
 * // Smoothly ease a health bar toward its target without overshooting:
 * displayedHp = FlixelMath.approach(displayedHp, actualHp, 120f * elapsed);
 *
 * // Clamp a value to a safe range:
 * speed = FlixelMath.clamp(speed, 0f, MAX_SPEED);
 *
 * // Linearly interpolate between two values:
 * float mid = FlixelMath.lerp(startX, endX, 0.5f);
 *
 * // Fast trig from a lookup table (tiny inaccuracy, big speed win):
 * float dx = FlixelMath.cos(angle) * speed;
 * float dy = FlixelMath.sin(angle) * speed;
 * }</pre>
 *
 * <h2>Randomness - FlixelRandom</h2>
 * <p>{@link org.flixelgdx.math.FlixelRandom FlixelRandom} is a seedable generator. The global
 * instance is {@link org.flixelgdx.Flixel#random Flixel.random}; create a local one with a
 * fixed seed for reproducible procedural generation:
 *
 * <pre>{@code
 * // Roll a random integer in [1, 6]:
 * int roll = Flixel.random.nextInt(1, 6);
 *
 * // Pick a random element from an array:
 * String name = Flixel.random.pick(nameList.getItems());
 * }</pre>
 *
 * <h2>Value types</h2>
 * <p>{@link org.flixelgdx.math.FlixelVector FlixelVector} and
 * {@link org.flixelgdx.math.FlixelRect FlixelRect} are mutable structs. They are poolable, so
 * use them from their dedicated pools when you need a temporary and want to avoid allocation,
 * like so:
 *
 * <pre>{@code
 * FlixelRect rect = FlixelRect.get();
 * // ...do some calculations...
 * rect.put();
 * }</pre>
 *
 * @see org.flixelgdx.math.FlixelMath
 * @see org.flixelgdx.math.FlixelRandom
 * @see org.flixelgdx.math.FlixelVector
 * @see org.flixelgdx.math.FlixelRect
 */
package org.flixelgdx.math;
