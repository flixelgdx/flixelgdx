/**
 * Group and collection types for FlixelGDX.
 *
 * <p>{@link org.flixelgdx.group.FlixelGroup FlixelGroup} is a generic {@link com.badlogic.gdx.utils.SnapshotArray}
 * wrapper for any member type (useful in plain libGDX projects). {@link org.flixelgdx.group.FlixelBasicGroup FlixelBasicGroup}
 * adds {@link org.flixelgdx.functional.IFlixelBasic IFlixelBasic} update/draw/recycle/destroy semantics.
 *
 * <p>{@link org.flixelgdx.group.FlixelGroup#remove FlixelGroup.remove} and {@link org.flixelgdx.group.FlixelGroupable#detach FlixelGroupable.detach}
 * only unlink members. For {@link org.flixelgdx.functional.IFlixelBasic IFlixelBasic} members, prefer
 * {@link org.flixelgdx.FlixelBasic#kill() FlixelBasic.kill()} / {@link org.flixelgdx.FlixelBasic#revive() FlixelBasic.revive()} or
 * {@link org.flixelgdx.group.FlixelBasicGroup#recycle() FlixelBasicGroup.recycle()}. See {@link org.flixelgdx.FlixelBasic FlixelBasic}
 * for a lifecycle table.
 *
 * <p>{@link org.flixelgdx.group.FlixelBasicGroupable FlixelBasicGroupable} marks groups whose members are
 * {@link org.flixelgdx.functional.IFlixelBasic IFlixelBasic} for engine utilities (overlap, debug traversal).
 *
 * @see org.flixelgdx.FlixelState
 * @see org.flixelgdx.FlixelBasic
 * @see org.flixelgdx.group.FlixelGroupable
 * @see org.flixelgdx.group.FlixelBasicGroupable
 */
package org.flixelgdx.group;
