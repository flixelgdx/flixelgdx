/**
 * Logical input actions: named {@link me.stringdotjar.flixelgdx.input.action.FlixelAction} instances grouped in a
 * {@link me.stringdotjar.flixelgdx.input.action.FlixelActionSet}, with tagged {@link me.stringdotjar.flixelgdx.input.action.FlixelInputBinding}
 * and {@link me.stringdotjar.flixelgdx.input.action.FlixelAnalogAxisBinding} so keyboard, gamepad, pointer, and touch regions
 * never share one ambiguous integer bucket.
 *
 * <h2>Why use this package</h2>
 *
 * <p>Calling {@code Flixel.keys.pressed(...)} everywhere works until you add rebinding, Steam Input, or extra devices.
 * Actions give you one place to change bindings ({@link me.stringdotjar.flixelgdx.input.action.FlixelActionDigital#addBinding})
 * while gameplay keeps asking {@code jump.justPressed()}.
 *
 * <h2>Frame order (important)</h2>
 *
 * <p>{@link me.stringdotjar.flixelgdx.FlixelGame#update(float)} runs {@code Flixel.keys.update()}, {@code Flixel.mouse.update()},
 * {@code Flixel.gamepads.update()}, then {@link me.stringdotjar.flixelgdx.input.action.FlixelActionSets#updateAll(float)}.
 * Your {@link me.stringdotjar.flixelgdx.FlixelState#update(float)} runs after that, so action queries see the current frame's
 * hardware snapshot. {@link me.stringdotjar.flixelgdx.FlixelGame#render()} ends with {@code endFrame()} on keys, mouse, gamepads,
 * then {@link me.stringdotjar.flixelgdx.input.action.FlixelActionSets#endFrameAll()} so {@code justPressed()} / {@code justReleased()}
 * match the rest of the engine.
 *
 * <h2>Minimal workflow</h2>
 *
 * <ol>
 *   <li>Subclass {@link me.stringdotjar.flixelgdx.input.action.FlixelActionSet}.</li>
 *   <li>Create {@link me.stringdotjar.flixelgdx.input.action.FlixelActionDigital} / {@link me.stringdotjar.flixelgdx.input.action.FlixelActionAnalog}, add bindings, {@code add(...)} each action.</li>
 *   <li>Hold the set while the mode is active; in {@code update} read {@code pressed()}, {@code justPressed()}, {@code getX()}.</li>
 *   <li>Call {@link me.stringdotjar.flixelgdx.input.action.FlixelActionSet#destroy()} when leaving the mode.</li>
 * </ol>
 *
 * <h2>Steam Input</h2>
 *
 * <p>Ship a manifest such as {@code me/stringdotjar/flixelgdx/input/action/steam_input_manifest.vdf} (resource in this module)
 * next to your game depot under Valve's {@code steam_input} layout. Action {@link me.stringdotjar.flixelgdx.input.action.FlixelAction#getName()}
 * strings should match manifest entries. At runtime, set {@link me.stringdotjar.flixelgdx.input.action.FlixelActionSet#steamReader}
 * to an implementation backed by steamworks4j (or another Steamworks binding) that reads Steam Input each frame;
 * {@link me.stringdotjar.flixelgdx.input.action.FlixelSteamActionReaders#EMPTY} is a safe default.
 */
package me.stringdotjar.flixelgdx.input.action;
