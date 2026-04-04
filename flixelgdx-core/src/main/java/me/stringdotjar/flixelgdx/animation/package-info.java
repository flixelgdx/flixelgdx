/**
 * Sprite animation controllers, state machines, and clip playback helpers used by {@link me.stringdotjar.flixelgdx.FlixelSprite}.
 *
 * <p>Controllers are optional: {@link me.stringdotjar.flixelgdx.FlixelSprite#ensureAnimation()} allocates a
 * {@link me.stringdotjar.flixelgdx.animation.FlixelAnimationController} when you need timelines, Sparrow/XML atlases, or named clips.
 * {@link me.stringdotjar.flixelgdx.FlixelSprite} doesn't include an animation controller by default because they
 * take up a large chunk of memory, which can add up quickly if you have a lot of sprites.
 *
 * <p>For simple flip-book style frames, prefer loading frame grids or atlases once and driving playback through the controller
 * rather than allocating new textures per frame.
 *
 * @see me.stringdotjar.flixelgdx.animation.FlixelAnimationController
 * @see me.stringdotjar.flixelgdx.animation.FlixelAnimationStateMachine
 */
package me.stringdotjar.flixelgdx.animation;
