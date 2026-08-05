/**
 * Math value types and helpers owned by FlixelGDX.
 *
 * <p>This package holds the framework's own math surface: the geometric value
 * types ({@link org.flixelgdx.math.FlixelPoint}, {@link org.flixelgdx.math.FlixelRect}),
 * the static math helpers in {@link org.flixelgdx.math.FlixelMathUtil}, and the
 * seedable random generator {@link org.flixelgdx.math.FlixelRandom}. These are
 * clean-room reimplementations that replace the libGDX math utilities the
 * framework used to lean on, so game code never has to touch a third-party math
 * API.
 *
 * <p>The designs here take cues from HaxeFlixel and libGDX (algorithms and API
 * shapes are not copyrightable), but every line is our own. A courtesy credit to
 * both projects is enough; there is no copied source to attribute.
 */
package org.flixelgdx.math;
