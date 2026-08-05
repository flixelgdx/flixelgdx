/**
 * Math value types and helpers owned by FlixelGDX.
 *
 * <p>This package holds the framework's math surface: the geometric value types
 * ({@link org.flixelgdx.math.FlixelVector}, {@link org.flixelgdx.math.FlixelRect}),
 * the static math helpers in {@link org.flixelgdx.math.FlixelMathUtil}, and the
 * seedable random generator {@link org.flixelgdx.math.FlixelRandom}. Together
 * they cover the arithmetic, geometry, and randomness game code needs without
 * reaching outside the framework.
 *
 * <p>Everything here is allocation-conscious: the value types are poolable and
 * the helpers are static and side-effect free, so they are safe to call inside
 * update and render loops.
 */
package org.flixelgdx.math;
