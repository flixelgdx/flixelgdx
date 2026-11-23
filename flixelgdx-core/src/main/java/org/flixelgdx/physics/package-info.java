/**
 * Collision detection internals used by {@link org.flixelgdx.Flixel#overlap(org.flixelgdx.FlixelBasic,
 * org.flixelgdx.FlixelBasic) Flixel.overlap(...)} and {@link org.flixelgdx.Flixel#collide(org.flixelgdx.FlixelBasic,
 * org.flixelgdx.FlixelBasic) Flixel.collide(...)}.
 *
 * <p>Collision checks happen in two steps. The broad phase quickly throws away pairs of objects that are too far
 * apart to touch, and the narrow phase runs the exact overlap test (and any callbacks) on the pairs that are left.
 * {@link org.flixelgdx.physics.FlixelCollisionGrid FlixelCollisionGrid} is the broad phase: it sorts objects into a
 * uniform grid of cells so each object is only tested against its neighbors, and falls back to testing every pair
 * when there are too few objects for the grid to pay off.
 *
 * <p>Most games never use this package directly. Call {@code Flixel.overlap(...)} and {@code Flixel.collide(...)}
 * instead:
 *
 * <pre>{@code
 * Flixel.collide(player, walls);
 * Flixel.overlap(bullets, enemies, onHit, null);
 * }</pre>
 */
package org.flixelgdx.physics;
