/**
 * Collection and pooling types owned by FlixelGDX.
 *
 * <p>This package is the framework's own replacement for the libGDX collection
 * and pooling utilities the framework used to depend on. It holds object pooling
 * ({@link org.flixelgdx.collections.FlixelPool},
 * {@link org.flixelgdx.collections.FlixelPoolable}) and, as Phase 1 of the
 * migration continues, the growable arrays, maps, and sets that game code and
 * the framework itself iterate every frame.
 *
 * <p>Everything here is built for the framework's no-per-frame-allocation rule:
 * backing arrays are reused, iteration is index based, and growth is amortized.
 * The designs take cues from HaxeFlixel and libGDX (algorithms are not
 * copyrightable), but every line is our own clean-room implementation.
 */
package org.flixelgdx.collections;
