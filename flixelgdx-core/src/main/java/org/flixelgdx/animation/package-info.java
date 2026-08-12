/**
 * Sprite animation controllers, state machines, and clip playback helpers used by
 * {@link org.flixelgdx.FlixelSprite FlixelSprite}.
 *
 * <p>Animation controllers are opt-in:
 * {@link org.flixelgdx.FlixelSprite#ensureAnimation() FlixelSprite.ensureAnimation()} allocates a
 * {@link org.flixelgdx.animation.FlixelAnimationController FlixelAnimationController} on demand
 * when you need named clips, Sparrow/XML atlases, or frame timelines. Sprites without an
 * animation controller pay none of the memory overhead, which matters when a scene contains
 * hundreds or thousands of them.
 *
 * <h2>Loading and playing clips</h2>
 * <p>The most common workflow is a Sparrow atlas (an XML file paired with a spritesheet image).
 * Load it once, register named clips from it, then play by name:
 *
 * <pre>{@code
 * // In create():
 * sprite = new FlixelSprite();
 * FlixelAnimationController anim = sprite.ensureAnimation();
 * // One-liner assuming hero.png and hero.xml exist next to each other in the same folder.
 * anim.addSparrowAtlas(Flixel.files.internal("images/hero"));
 * anim.addByPrefix("idle",   "hero_idle_",   12, true);
 * anim.addByPrefix("run",    "hero_run_",    24, true);
 * anim.addByPrefix("attack", "hero_attack_", 24, false);
 * anim.play("idle");
 * }</pre>
 *
 * <h2>State machines</h2>
 * <p>When animation rules grow complex - one-shot attack clips that return to idle, transitions
 * that are only legal from certain states, enter/exit callbacks - use
 * {@link org.flixelgdx.animation.FlixelAnimationStateMachine FlixelAnimationStateMachine} instead
 * of scattering {@code playAnimation(...)} calls across gameplay code. Attach the machine to the
 * controller via
 * {@link org.flixelgdx.animation.FlixelAnimationController#setStateMachine(org.flixelgdx.animation.FlixelAnimationStateMachine) FlixelAnimationController.setStateMachine(...)}
 * so it is ticked automatically:
 *
 * <pre>{@code
 * FlixelAnimationStateMachine machine = new FlixelAnimationStateMachine(sprite.ensureAnimation());
 * machine.addState("idle",   "idle",   true).allowTo("run", "attack");
 * machine.addState("run",    "run",    true).allowTo("idle", "attack");
 * machine.addState("attack", "attack", false).autoAdvanceTo("idle");
 * machine.start("idle");
 * sprite.ensureAnimation().setStateMachine(machine);
 * }</pre>
 *
 * <h2>Performance note</h2>
 * <p>For simple static sprites that never animate, do not call
 * {@link org.flixelgdx.FlixelSprite#ensureAnimation() FlixelSprite.ensureAnimation()}. The
 * controller holds several collections internally, and allocating one per sprite in a large group
 * adds up quickly.
 *
 * @see org.flixelgdx.animation.FlixelAnimationController
 * @see org.flixelgdx.animation.FlixelAnimationStateMachine
 */
package org.flixelgdx.animation;
