/**
 * Group and collection types for FlixelGDX.
 *
 * <p>{@link me.stringdotjar.flixelgdx.group.FlixelGroup} is a generic {@link com.badlogic.gdx.utils.SnapshotArray}
 * wrapper for any member type (useful in plain libGDX projects). {@link me.stringdotjar.flixelgdx.group.FlixelBasicGroup}
 * adds {@link me.stringdotjar.flixelgdx.FlixelBasic} update/draw/recycle/destroy semantics.
 *
 * <p>{@link me.stringdotjar.flixelgdx.group.FlixelGroup#remove} and {@link FlixelGroupable#detach} only unlink members.
 * For {@link me.stringdotjar.flixelgdx.FlixelBasic} members, prefer {@link me.stringdotjar.flixelgdx.FlixelBasic#kill()} /
 * {@link me.stringdotjar.flixelgdx.FlixelBasic#revive()} or {@link FlixelBasicGroup#recycle()}. See
 * {@link me.stringdotjar.flixelgdx.FlixelBasic} for a lifecycle table.
 *
 * <p>{@link FlixelBasicGroupable} marks groups whose members are {@link me.stringdotjar.flixelgdx.FlixelBasic} for engine
 * utilities (overlap, debug traversal).
 *
 * @see me.stringdotjar.flixelgdx.FlixelState
 * @see me.stringdotjar.flixelgdx.FlixelBasic
 * @see me.stringdotjar.flixelgdx.group.FlixelGroupable
 * @see me.stringdotjar.flixelgdx.group.FlixelBasicGroupable
 */
package me.stringdotjar.flixelgdx.group;
