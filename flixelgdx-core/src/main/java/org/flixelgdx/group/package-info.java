/**
 * Group and collection types for organizing and managing game objects in FlixelGDX.
 *
 * <p>Think of a group like a shopping basket: it holds items together so you can update,
 * draw, or act on all of them at once without juggling each one individually. Depending on
 * how much engine integration you need, there are five types to choose from:
 *
 * <ul>
 *   <li>{@link org.flixelgdx.group.FlixelGroupable FlixelGroupable} - the base interface; any
 *       container with add, remove, clear, and snapshot iteration. Not tied to the FlixelGDX
 *       lifecycle.</li>
 *   <li>{@link org.flixelgdx.group.FlixelGroup FlixelGroup} - a concrete, framework-agnostic
 *       member list backed by a {@link org.flixelgdx.collections.FlixelArray FlixelArray}.
 *       Works with any type; update and draw are the caller's responsibility.</li>
 *   <li>{@link org.flixelgdx.group.FlixelBasicGroupable FlixelBasicGroupable} - extends
 *       {@link org.flixelgdx.group.FlixelGroupable FlixelGroupable} for
 *       {@link org.flixelgdx.functional.IFlixelBasic IFlixelBasic} members; adds dead-member
 *       queries and optional destroy-on-remove.</li>
 *   <li>{@link org.flixelgdx.group.FlixelBasicGroup FlixelBasicGroup} - a full
 *       {@link org.flixelgdx.FlixelBasic FlixelBasic} that automatically calls update and draw
 *       on every active member each frame, and destroys all members when the group itself is
 *       destroyed.</li>
 *   <li>{@link org.flixelgdx.group.FlixelSpriteGroup FlixelSpriteGroup} - a
 *       {@link org.flixelgdx.FlixelSprite FlixelSprite} that propagates position, alpha, color,
 *       scale, flip, and rotation to all of its sprite children, so the group behaves as one
 *       visual unit.</li>
 * </ul>
 *
 * <h2>Choosing the right type</h2>
 * <p>A quick rule of thumb: use {@link org.flixelgdx.group.FlixelGroup FlixelGroup} when you
 * just need an organized list and will drive iteration yourself. Use
 * {@link org.flixelgdx.group.FlixelBasicGroup FlixelBasicGroup} when the members are game
 * objects that need the standard update and draw loop. Use
 * {@link org.flixelgdx.group.FlixelSpriteGroup FlixelSpriteGroup} when the group itself must
 * move, scale, or fade as a single visual thing.
 *
 * <h2>Basic group usage</h2>
 * <p>The most common pattern is to add a
 * {@link org.flixelgdx.group.FlixelBasicGroup FlixelBasicGroup} inside a
 * {@link org.flixelgdx.FlixelState FlixelState} and populate it with your objects:
 *
 * <pre>{@code
 * // Inside a FlixelState:
 * FlixelBasicGroup<FlixelSprite> enemies = new FlixelBasicGroup<>(FlixelSprite[]::new);
 * add(enemies);
 *
 * // Spawn an enemy:
 * FlixelSprite enemy = new FlixelSprite();
 * enemy.setPosition(200f, 100f);
 * enemies.add(enemy);
 *
 * // Iterate through the group.
 * // Make sure to reuse the lambda if using inside of hot loops!
 * enemies.forEachMember(e -> {
 *   if (e == null) {
 *     return;
 *   }
 *   if (e.getX() > screenWidth) {
 *     e.kill();
 *   }
 * });
 * }</pre>
 *
 * <h2>Recycling members</h2>
 * <p>Allocating a new object for every bullet, particle, or enemy is expensive. The recycle
 * pattern reuses dead (killed) members instead of throwing them away. Think of it like a
 * pool of rubber ducks: you grab a duck that is not in the water, clean it up, and throw
 * it back in rather than buying a new one each time.
 *
 * <pre>{@code
 * // Set a cap so the pool never grows past 50 bullets:
 * FlixelBasicGroup<FlixelSprite> bullets = new FlixelBasicGroup<>(FlixelSprite[]::new, 50);
 * add(bullets);
 *
 * // Fire a bullet (reuses a dead one, or does nothing when at capacity):
 * FlixelSprite bullet = bullets.recycle();
 * if (bullet != null) {
 *   bullet.setPosition(playerX, playerY);
 * }
 *
 * // When a bullet goes off-screen, kill it so recycle() can pick it up later:
 * if (bullet.getX() > screenWidth) {
 *   bullet.kill();
 * }
 * }</pre>
 *
 * <h2>FlixelSpriteGroup and visual propagation</h2>
 * <p>A {@link org.flixelgdx.group.FlixelSpriteGroup FlixelSpriteGroup} acts as a single
 * drawable unit. Moving, tinting, or scaling the group automatically transforms every child
 * sprite. Because it extends {@link org.flixelgdx.FlixelSprite FlixelSprite}, groups can be
 * nested inside other groups for hierarchical compositions.
 *
 * <pre>{@code
 * FlixelSpriteGroup ship = new FlixelSpriteGroup();
 * ship.setPosition(100f, 200f);
 *
 * FlixelSprite body = new FlixelSprite();
 * FlixelSprite cannon = new FlixelSprite();
 * ship.add(body);
 * ship.add(cannon);
 *
 * // Fade the entire ship in one call:
 * ship.setAlpha(0.5f);
 *
 * // Move the whole group; both body and cannon follow automatically:
 * ship.setPosition(150f, 250f);
 * }</pre>
 *
 * <p>Rotation behavior is controlled per group via
 * {@link org.flixelgdx.group.FlixelSpriteGroup.RotationMode FlixelSpriteGroup.RotationMode}:
 * <ul>
 *   <li>{@link org.flixelgdx.group.FlixelSpriteGroup.RotationMode#INDIVIDUAL INDIVIDUAL}
 *       (default) - applies the rotation delta to each sprite's own rotation; no positional
 *       changes occur.</li>
 *   <li>{@link org.flixelgdx.group.FlixelSpriteGroup.RotationMode#WHEEL WHEEL} - repositions
 *       sprites evenly around the group center each frame at the configured radius; useful for
 *       orbiting bullet patterns.</li>
 *   <li>{@link org.flixelgdx.group.FlixelSpriteGroup.RotationMode#ORBIT ORBIT} - rotates each
 *       sprite's position around the group origin like a rigid body, also adjusting each
 *       sprite's own angle.</li>
 * </ul>
 *
 * <h2>Remove, detach, kill, and destroy</h2>
 * <p>These four operations are not interchangeable:
 * <ul>
 *   <li>{@link org.flixelgdx.group.FlixelGroupable#remove remove(member)} and
 *       {@link org.flixelgdx.group.FlixelGroupable#detach detach(member)} - unlink the member
 *       from the group; the member itself is left untouched.</li>
 *   <li>{@link org.flixelgdx.FlixelBasic#kill() FlixelBasic.kill()} - marks the member as dead
 *       and inactive without removing it from the group;
 *       {@link org.flixelgdx.group.FlixelBasicGroup#recycle() recycle()} can revive it.</li>
 *   <li>{@link org.flixelgdx.group.FlixelBasicGroupable#removeMember removeMember(member, true)}
 *       - unlinks and then calls
 *       {@link org.flixelgdx.FlixelBasic#destroy() destroy()} on the member, freeing its
 *       resources.</li>
 *   <li>{@link org.flixelgdx.group.FlixelBasicGroup#destroy() FlixelBasicGroup.destroy()} -
 *       destroys the group and every member inside it; use only when the group is no longer
 *       needed.</li>
 * </ul>
 * <p>Prefer {@link org.flixelgdx.FlixelBasic#kill() FlixelBasic.kill()} and
 * {@link org.flixelgdx.group.FlixelBasicGroup#recycle() recycle()} over destroy and re-create
 * for objects that come and go frequently (enemies, bullets, particles).
 *
 * @see org.flixelgdx.group.FlixelGroupable
 * @see org.flixelgdx.group.FlixelBasicGroupable
 * @see org.flixelgdx.group.FlixelGroup
 * @see org.flixelgdx.group.FlixelBasicGroup
 * @see org.flixelgdx.group.FlixelSpriteGroup
 * @see org.flixelgdx.FlixelState
 * @see org.flixelgdx.FlixelBasic
 */
package org.flixelgdx.group;
