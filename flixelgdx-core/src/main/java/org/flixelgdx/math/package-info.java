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
 * use them from their dedicated pools when you need a temporary object and want to avoid allocation,
 * like so:
 *
 * <pre>{@code
 * FlixelRect rect = FlixelRect.get();
 * // ...do some calculations...
 * rect.put();
 * }</pre>
 *
 * <h2>Bounds and ranges</h2>
 * <p>Two small families describe values that vary. Bounds say "pick something between these";
 * ranges say "slide from this to that over a lifetime":
 *
 * <ul>
 *   <li>{@link org.flixelgdx.math.FlixelBounds FlixelBounds} and
 *       {@link org.flixelgdx.math.FlixelPointBounds FlixelPointBounds} - a minimum and a maximum
 *       to roll a random number or point between.</li>
 *   <li>{@link org.flixelgdx.math.FlixelRange FlixelRange} and
 *       {@link org.flixelgdx.math.FlixelPointRange FlixelPointRange} - a start and an end that
 *       something blends between as it ages.</li>
 *   <li>{@link org.flixelgdx.math.FlixelRangeBounds FlixelRangeBounds} and
 *       {@link org.flixelgdx.math.FlixelPointRangeBounds FlixelPointRangeBounds} - the recipe for
 *       a range: bounds for the start, bounds for the end, and an optional easing curve.</li>
 * </ul>
 *
 * <p>Particle emitters are the main user, but the types work anywhere you want controlled
 * randomness, such as spawners or procedural effects. The color versions live next to
 * {@link org.flixelgdx.util.FlixelColor FlixelColor} in {@link org.flixelgdx.util}.
 *
 * <pre>{@code
 * // Each enemy spawns with a slightly different speed.
 * FlixelBounds enemySpeed = new FlixelBounds(80f, 120f);
 * float speed = Flixel.random.nextFloat(enemySpeed.min, enemySpeed.max);
 *
 * // Shrink from full size to nothing over one second.
 * FlixelRange shrink = new FlixelRange(1f, 0f);
 * sprite.setScale(shrink.lerp(elapsedSoFar / 1f));
 * }</pre>
 *
 * @see org.flixelgdx.math.FlixelMath
 * @see org.flixelgdx.math.FlixelRandom
 * @see org.flixelgdx.math.FlixelVector
 * @see org.flixelgdx.math.FlixelRect
 */
package org.flixelgdx.math;
