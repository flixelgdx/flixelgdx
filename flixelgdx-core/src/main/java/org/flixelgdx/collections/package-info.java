/**
 * Collection and pooling types owned by FlixelGDX.
 *
 * <p>This package holds the framework's general-purpose containers: object pooling
 * ({@link org.flixelgdx.collections.FlixelPool}, {@link org.flixelgdx.collections.FlixelPoolable})
 * plus growable arrays, maps, and sets that game code and the framework itself iterate every frame.
 *
 * <h2>Always prefer these over standard Java collections</h2>
 * <p>Standard Java collections ({@code ArrayList}, {@code HashMap}, {@code HashSet}, etc.) are
 * memory heavy and create too much garbage during normal use. They allocate iterator objects on
 * every enhanced for-loop, carry heavy internal overhead, and produce garbage that forces the
 * garbage collector to pause the game at the worst possible moments. The types in this package
 * are specifically designed to avoid those problems. They are backed by plain arrays, support
 * index-based iteration with zero allocations, and consume far less memory.
 *
 * <h2>Growable arrays</h2>
 * <p>{@link org.flixelgdx.collections.FlixelArray FlixelArray} is the general-purpose ordered
 * list for objects. Primitive variants ({@link org.flixelgdx.collections.FlixelIntArray},
 * {@link org.flixelgdx.collections.FlixelFloatArray}, {@link org.flixelgdx.collections.FlixelLongArray},
 * and friends) avoid boxing entirely. Always iterate with an index-based loop to keep the hot
 * path allocation-free:
 *
 * <pre>{@code
 * FlixelArray<Enemy> enemies = new FlixelArray<>(Enemy[]::new);
 * enemies.add(new Enemy());
 *
 * // Zero-allocation iteration. Use this pattern everywhere:
 * Enemy[] items = enemies.getItems();
 * for (int i = 0; i < enemies.getSize(); i++) {
 *   items[i].update(elapsed);
 * }
 * }</pre>
 *
 * <h2>Maps and sets</h2>
 * <p>{@link org.flixelgdx.collections.FlixelMap FlixelMap} stores key-value pairs for object
 * keys. {@link org.flixelgdx.collections.FlixelIntMap FlixelIntMap} and
 * {@link org.flixelgdx.collections.FlixelLongMap FlixelLongMap} use primitive keys directly,
 * avoiding the boxing that {@code HashMap<Integer, V>} requires.
 * {@link org.flixelgdx.collections.FlixelSet FlixelSet},
 * {@link org.flixelgdx.collections.FlixelIntSet FlixelIntSet}, and
 * {@link org.flixelgdx.collections.FlixelLongSet FlixelLongSet} provide the corresponding set
 * types.
 *
 * <h2>Object pooling</h2>
 * <p>{@link org.flixelgdx.collections.FlixelPool FlixelPool} eliminates allocation spikes for
 * short-lived objects like bullets and particles. Borrow with
 * {@link org.flixelgdx.collections.FlixelPool#obtain() FlixelPool.obtain()}, then hand back
 * with {@link org.flixelgdx.collections.FlixelPool#free(Object) FlixelPool.free(...)} when done.
 * If the pooled type implements {@link org.flixelgdx.collections.FlixelPoolable FlixelPoolable},
 * its {@link org.flixelgdx.collections.FlixelPoolable#reset() reset()} method runs automatically
 * on free so no stale state carries over:
 *
 * <pre>{@code
 * FlixelPool<Bullet> pool = new FlixelPool<>() {
 *   @Override
 *   protected Bullet newObject() {
 *     return new Bullet();
 *   }
 * };
 *
 * // When firing:
 * Bullet b = pool.obtain();
 * b.reset(x, y, speed);
 *
 * // When the bullet expires:
 * pool.free(b);
 * }</pre>
 *
 * @see org.flixelgdx.collections.FlixelArray
 * @see org.flixelgdx.collections.FlixelMap
 * @see org.flixelgdx.collections.FlixelPool
 */
package org.flixelgdx.collections;
