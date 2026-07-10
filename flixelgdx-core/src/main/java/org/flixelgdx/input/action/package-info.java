/**
 * Logical input actions: named {@link org.flixelgdx.input.action.FlixelAction FlixelAction} instances grouped in a
 * {@link org.flixelgdx.input.action.FlixelActionSet FlixelActionSet}, wired up with
 * {@link org.flixelgdx.input.action.FlixelDigitalBinding FlixelDigitalBinding} and
 * {@link org.flixelgdx.input.action.FlixelAnalogBinding FlixelAnalogBinding} contributors.
 *
 * <h2>Why use this package</h2>
 *
 * <p>Calling {@code Flixel.keys.pressed(...)} everywhere works until you add rebinding, Steam Input, or extra devices.
 * Actions give you one place to change bindings ({@link org.flixelgdx.input.action.FlixelActionDigital#addBinding FlixelActionDigital.addBinding})
 * while gameplay keeps asking {@code jump.justPressed()}.
 *
 * <h2>Frame order (important)</h2>
 *
 * <p>{@link org.flixelgdx.FlixelGame#update(float) FlixelGame.update(float)} runs {@code Flixel.keys.update()}, {@code Flixel.mouse.update()},
 * {@code Flixel.gamepads.update()}, then {@link org.flixelgdx.input.action.FlixelActionSets#update(float) FlixelActionSets.updateAll(float)}.
 * Your {@link org.flixelgdx.FlixelState#update(float) FlixelState.update(float)} runs after that, so action queries see the current frame's
 * hardware snapshot. {@link org.flixelgdx.FlixelGame#render() FlixelGame.render()} ends with {@code endFrame()} on keys, mouse, gamepads,
 * then {@link org.flixelgdx.input.action.FlixelActionSets#endFrameAll() FlixelActionSets.endFrameAll()} so {@code justPressed()} / {@code justReleased()}
 * match the rest of the engine.
 *
 * <h2>Minimal workflow</h2>
 *
 * <ol>
 *   <li>Subclass {@link org.flixelgdx.input.action.FlixelActionSet FlixelActionSet}.</li>
 *   <li>Create {@link org.flixelgdx.input.action.FlixelActionDigital FlixelActionDigital} / {@link org.flixelgdx.input.action.FlixelActionAnalog FlixelActionAnalog}, add bindings, {@code add(...)} each action.</li>
 *   <li>Hold the set while the mode is active; in {@code update} read {@code pressed()}, {@code justPressed()}, {@code getX()}.</li>
 *   <li>Call {@link org.flixelgdx.input.action.FlixelActionSet#destroy() FlixelActionSet.destroy()} when leaving the mode.</li>
 * </ol>
 *
 * <h2>Steam Input</h2>
 *
 * <p>Ship a manifest such as {@code org/flixelgdx/input/action/steam_input_manifest.vdf} (resource in this module)
 * next to your game depot under Valve's {@code steam_input} layout. Action {@link org.flixelgdx.input.action.FlixelAction#getName() FlixelAction.getName()}
 * strings should match manifest entries. At runtime, set {@link org.flixelgdx.input.action.FlixelActionSet#steamReader FlixelActionSet.steamReader}
 * to an implementation backed by steamworks4j (or another Steamworks binding) that reads Steam Input each frame;
 * {@link org.flixelgdx.input.action.FlixelSteamActionReaders#EMPTY FlixelSteamActionReaders.EMPTY} is a safe default.
 */
package org.flixelgdx.input.action;
