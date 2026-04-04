/**
 * Group and collection types for FlixelGDX.
 *
 * <p>This package provides container classes and interfaces for creating and managing group objects.
 * Groups are used to manage collections of {@link me.stringdotjar.flixelgdx.FlixelBasic} objects,
 * update them, and optionally draw them.
 *
 * <p>{@link me.stringdotjar.flixelgdx.group.FlixelGroup} and {@link me.stringdotjar.flixelgdx.group.FlixelSpriteGroup}
 * require a non-null {@link com.badlogic.gdx.utils.Pool}. {@link FlixelGroup#remove} returns members to that pool
 * (after {@link me.stringdotjar.flixelgdx.FlixelBasic#destroy} via pool {@code reset}). Use
 * {@link me.stringdotjar.flixelgdx.group.FlixelBasicGroupable#obtainMember()} or {@link FlixelGroup#recycle()} for hot paths..
 *
 * <p>Groups are commonly used in {@link me.stringdotjar.flixelgdx.FlixelState} to organize game
 * objects, UI elements, and effects.
 *
 * @see me.stringdotjar.flixelgdx.FlixelState
 * @see me.stringdotjar.flixelgdx.group.FlixelBasicGroupable
 */
package me.stringdotjar.flixelgdx.group;
