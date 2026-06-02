/**
 * Gamepad input support for FlixelGDX.
 *
 * <p>{@link org.flixelgdx.input.gamepad.FlixelGamepadModel} describes the detected
 * controller family (for UI prompts and telemetry). Logical buttons and axes are defined on
 * {@link org.flixelgdx.input.gamepad.FlixelGamepadInput} and resolved per device through
 * gdx-controllers {@link com.badlogic.gdx.controllers.Controller#getMapping()}. The global
 * {@link org.flixelgdx.Flixel#gamepads} manager is wired from {@link org.flixelgdx.FlixelGame}
 * each frame.
 */
package org.flixelgdx.input.gamepad;
