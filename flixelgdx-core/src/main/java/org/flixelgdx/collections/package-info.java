/**
 * Collection and pooling types owned by FlixelGDX.
 *
 * <p>This package holds the framework's general-purpose containers: object
 * pooling ({@link org.flixelgdx.collections.FlixelPool},
 * {@link org.flixelgdx.collections.FlixelPoolable}) plus the growable arrays,
 * maps, and sets that game code and the framework itself iterate every frame.
 *
 * <p>Everything here is designed to be light and to not produce allocations
 * every frame when being used. It's highly advised you use this collection
 * system over standard Java's, which consumes more memory and allocates
 * objects when being iterated, especially in hot loops.
 */
package org.flixelgdx.collections;
