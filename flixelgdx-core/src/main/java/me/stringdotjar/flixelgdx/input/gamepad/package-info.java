/**
 * Gamepad input support for FlixelGDX.
 *
 * <p>{@link me.stringdotjar.flixelgdx.input.gamepad.FlixelGamepadModel} describes the detected
 * controller family (for UI prompts and telemetry). Logical buttons and axes are defined on
 * {@link me.stringdotjar.flixelgdx.input.gamepad.FlixelGamepadInput} and resolved per device through
 * gdx-controllers {@link com.badlogic.gdx.controllers.Controller#getMapping()}. The global
 * {@link me.stringdotjar.flixelgdx.Flixel#gamepads} manager is wired from {@link me.stringdotjar.flixelgdx.FlixelGame}
 * each frame.
 */
package me.stringdotjar.flixelgdx.input.gamepad;
