/**
 * Gamepad input and haptics support for FlixelGDX.
 *
 * <p>{@link org.flixelgdx.input.gamepad.FlixelGamepadInputManager FlixelGamepadInputManager} is the global
 * entry point, accessible via {@link org.flixelgdx.Flixel#gamepads Flixel.gamepads}. It polls
 * controllers each frame and exposes familiar pressed/justPressed/justReleased semantics
 * alongside analog axis reads.
 *
 * <p>Logical button and axis constants live on
 * {@link org.flixelgdx.input.gamepad.FlixelGamepadButton FlixelGamepadButton} and are resolved to
 * native hardware indices through each
 * {@link com.badlogic.gdx.controllers.Controller#getMapping() Controller.getMapping()} at query
 * time. {@link org.flixelgdx.input.gamepad.FlixelGamepadModel FlixelGamepadModel} identifies the
 * controller family (PS4, Xbox, Switch Pro, etc.) so games can display the right button prompts.
 *
 * <p>The optional per-slot {@link org.flixelgdx.input.gamepad.FlixelGamepadDevice FlixelGamepadDevice}
 * facade exposes the same queries without passing a slot id on every call. Create one via
 * {@link org.flixelgdx.input.gamepad.FlixelGamepadInputManager#ensureDevice(int)}.
 *
 * <h2>Haptics and vibration</h2>
 *
 * <p>Vibration is routed through a pluggable
 * {@link org.flixelgdx.input.gamepad.FlixelHapticsProvider FlixelHapticsProvider}. Each backend
 * launcher installs the correct platform implementation automatically:
 *
 * <ul>
 *   <li>Desktop ({@code flixelgdx-lwjgl3}): uses Jamepad's SDL rumble API, which exposes
 *       independent left (low-frequency) and right (high-frequency) motor channels via
 *       {@code ControllerIndex.doVibration}.
 *   <li>Web ({@code flixelgdx-teavm}): calls the W3C Gamepad Haptics API
 *       ({@code vibrationActuator.playEffect('dual-rumble', ...)}) with separate
 *       {@code strongMagnitude} (left motor) and {@code weakMagnitude} (right motor) values.
 *       Requires a Chromium-based browser; Firefox does not support {@code vibrationActuator}.
 * </ul>
 *
 * <p>To use vibration, enable the gamepad system and call one of the {@code vibrate} methods:
 *
 * <pre>{@code
 * Flixel.gamepads.enabled = true;
 *
 * // Full-strength rumble on both motors for 0.3 seconds.
 * Flixel.gamepads.vibrate(0, 0.3f);
 *
 * // Left motor only at half strength for 0.5 seconds.
 * Flixel.gamepads.vibrate(0, 0.5f, 0f, 0.5f);
 * }</pre>
 *
 * <p>For platform-specific haptics (for example DualSense adaptive triggers), supply a custom
 * {@link org.flixelgdx.input.gamepad.FlixelHapticsProvider} via
 * {@link org.flixelgdx.input.gamepad.FlixelGamepadInputManager#setHapticsProvider}.
 */
package org.flixelgdx.input.gamepad;
