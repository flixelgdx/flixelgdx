/**
 * Collection and pooling types owned by FlixelGDX.
 *
 * <p>This package holds the framework's general-purpose containers: object
 * pooling ({@link org.flixelgdx.collections.FlixelPool},
 * {@link org.flixelgdx.collections.FlixelPoolable}) plus the growable arrays,
 * maps, and sets that game code and the framework itself iterate every frame.
 *
 * <p>Everything here is built for the framework's no-per-frame-allocation rule:
 * backing arrays are reused, iteration is index based, growth is amortized, and
 * the hash containers hand out reusable iterators. Prefer the public
 * {@code items}/{@code size} fields on the array types for the tightest loops.
 */
package org.flixelgdx.collections;
