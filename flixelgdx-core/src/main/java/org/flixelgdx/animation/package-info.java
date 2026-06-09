/**
 * Sprite animation controllers, state machines, and clip playback helpers used by {@link org.flixelgdx.FlixelSprite}.
 *
 * <p>Controllers are optional: {@link org.flixelgdx.FlixelSprite#ensureAnimation()} allocates a
 * {@link org.flixelgdx.animation.FlixelAnimationController} when you need timelines, Sparrow/XML atlases, or named clips.
 * {@link org.flixelgdx.FlixelSprite} doesn't include an animation controller by default because they
 * take up a large chunk of memory, which can add up quickly if you have a lot of sprites.
 *
 * <p>For simple flip-book style frames, prefer loading frame grids or atlases once and driving playback through the controller
 * rather than allocating new textures per frame.
 *
 * @see org.flixelgdx.animation.FlixelAnimationController
 * @see org.flixelgdx.animation.FlixelAnimationStateMachine
 */
package org.flixelgdx.animation;
