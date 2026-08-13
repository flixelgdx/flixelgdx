/**
 * Gamepad support for FlixelGDX: polling, logical button and axis tokens, controller detection,
 * haptic feedback, and a pluggable mapping system for exotic hardware.
 *
 * <h2>Key types</h2>
 *
 * <ul>
 *   <li>{@link org.flixelgdx.input.gamepad.FlixelGamepadInputManager FlixelGamepadInputManager} -
 *       the main manager, on {@link org.flixelgdx.Flixel#gamepads Flixel.gamepads}.</li>
 *   <li>{@link org.flixelgdx.input.gamepad.FlixelGamepadButton FlixelGamepadButton} - logical
 *       button tokens (A, B, X, Y, L1, L2, etc.).</li>
 *   <li>{@link org.flixelgdx.input.gamepad.FlixelGamepadAxis FlixelGamepadAxis} - logical axis
 *       tokens (LEFT_X, LEFT_Y, RIGHT_X, RIGHT_Y, L2, R2).</li>
 *   <li>{@link org.flixelgdx.input.gamepad.FlixelGamepadModel FlixelGamepadModel} - controller
 *       family identification (PS4, Xbox, Switch Pro, etc.).</li>
 *   <li>{@link org.flixelgdx.input.gamepad.FlixelGamepadDevice FlixelGamepadDevice} - optional
 *       per-slot facade, created with
 *       {@link org.flixelgdx.input.gamepad.FlixelGamepadInputManager#ensureDevice(int)
 *       ensureDevice(int)}.</li>
 *   <li>{@link org.flixelgdx.input.gamepad.FlixelGamepad FlixelGamepad} - single controller
 *       backend interface, implemented by each platform.</li>
 *   <li>{@link org.flixelgdx.input.gamepad.FlixelGamepadMapping FlixelGamepadMapping} - stores
 *       native button and axis indices for one controller slot.</li>
 *   <li>{@link org.flixelgdx.input.gamepad.FlixelGamepadMappingResolver FlixelGamepadMappingResolver} -
 *       interface for resolving a mapping when a controller connects.</li>
 *   <li>{@link org.flixelgdx.input.gamepad.FlixelGamepadDetector FlixelGamepadDetector} - detects
 *       the {@link org.flixelgdx.input.gamepad.FlixelGamepadModel FlixelGamepadModel} from VID/PID
 *       or name.</li>
 *   <li>{@link org.flixelgdx.input.gamepad.FlixelGamepadHapticsProvider FlixelGamepadHapticsProvider} -
 *       pluggable vibration backend.</li>
 *   <li>{@link org.flixelgdx.input.gamepad.FlixelGamepadListener FlixelGamepadListener} - event
 *       callbacks for connection, disconnection, and raw input.</li>
 * </ul>
 *
 * <h2>Quick start</h2>
 *
 * <p>The gamepad system is enabled by default. Use
 * {@link org.flixelgdx.input.gamepad.FlixelGamepadButton FlixelGamepadButton} tokens and slot IDs
 * (0 = first controller) to read input:
 *
 * <pre>{@code
 * // Held this frame on the first controller.
 * if (Flixel.gamepads.pressed(0, FlixelGamepadButton.A)) {
 *   player.jump();
 * }
 *
 * // True only on the first frame the button goes down on any controller.
 * if (Flixel.gamepads.anyJustPressed(FlixelGamepadButton.START)) {
 *   pauseGame();
 * }
 *
 * // Smooth analog movement from the left stick.
 * float moveX = Flixel.gamepads.getAxis(0, FlixelGamepadAxis.LEFT_X);
 * float moveY = Flixel.gamepads.getAxis(0, FlixelGamepadAxis.LEFT_Y);
 *
 * // Trigger pressure in the range [0, 1].
 * float accel = Flixel.gamepads.getTriggerR(0);
 * }</pre>
 *
 * <h2>Logical versus native input</h2>
 *
 * <p>Different controllers report their buttons and axes on different native indices. For example,
 * the "A" button on an Xbox controller might be native button 0, but on a PlayStation controller
 * the equivalent cross button could be native button 2.
 * {@link org.flixelgdx.input.gamepad.FlixelGamepadButton FlixelGamepadButton} and
 * {@link org.flixelgdx.input.gamepad.FlixelGamepadAxis FlixelGamepadAxis} are <em>logical</em>
 * tokens that always mean "the A button" or "the left stick X axis," regardless of the hardware.
 * The {@link org.flixelgdx.input.gamepad.FlixelGamepadMapping FlixelGamepadMapping} for each slot
 * translates these logical names to the native indices the connected controller actually uses.
 *
 * <p>The mapping is resolved once when a controller connects, using the
 * {@link org.flixelgdx.input.gamepad.FlixelGamepadMappingResolver FlixelGamepadMappingResolver}
 * chain. The framework includes a built-in resolver for common controllers. To support additional
 * or custom hardware, add your own resolver:
 *
 * <pre>{@code
 * Flixel.gamepads.addMappingResolver(gamepad -> {
 *   if (gamepad.getVendorId() == MY_VID && gamepad.getProductId() == MY_PID) {
 *     FlixelGamepadMapping m = new FlixelGamepadMapping();
 *     m.setButton(FlixelGamepadButton.A, 0);
 *     m.setButton(FlixelGamepadButton.B, 1);
 *     m.setAxis(FlixelGamepadAxis.LEFT_X, 0);
 *     m.setAxis(FlixelGamepadAxis.LEFT_Y, 1);
 *     return m;
 *   }
 *   return null; // fall through to the next resolver
 * });
 * }</pre>
 *
 * <p>User-added resolvers are checked first and take priority over framework-built-in resolvers.
 *
 * <h2>Controller identification and button prompts</h2>
 *
 * <p>{@link org.flixelgdx.input.gamepad.FlixelGamepadModel FlixelGamepadModel} identifies the
 * gamepad family so the game can display the right button prompts (PlayStation circles, Xbox
 * letters, Nintendo labels, etc.). The model is detected automatically from USB vendor and product
 * IDs via {@link org.flixelgdx.input.gamepad.FlixelGamepadDetector FlixelGamepadDetector}:
 *
 * <pre>{@code
 * Flixel.gamepads.deviceConnected.add(event -> {
 *   int slot = event.gamepadId();
 *   FlixelGamepadModel model = event.model();
 *   promptRenderer.setController(slot, model);
 * });
 * }</pre>
 *
 * <h2>Connection and disconnection events</h2>
 *
 * <p>Subscribe to
 * {@link org.flixelgdx.input.gamepad.FlixelGamepadInputManager#deviceConnected deviceConnected}
 * and
 * {@link org.flixelgdx.input.gamepad.FlixelGamepadInputManager#deviceDisconnected deviceDisconnected}
 * signals to react when controllers are plugged or unplugged at runtime. The payload objects are
 * reused across calls; do not retain them past the callback:
 *
 * <pre>{@code
 * Flixel.gamepads.deviceConnected.add(event -> {
 *   Flixel.info("Controller " + event.gamepadId() + " connected: " + event.model());
 * });
 * Flixel.gamepads.deviceDisconnected.subscribe(event -> {
 *   Flixel.info("Controller " + event.gamepadId() + " disconnected.");
 * });
 * }</pre>
 *
 * <h2>Per-slot facade</h2>
 *
 * <p>If you prefer to hold a reference to a specific slot rather than passing the slot ID on every
 * call, create a {@link org.flixelgdx.input.gamepad.FlixelGamepadDevice FlixelGamepadDevice}
 * facade with
 * {@link org.flixelgdx.input.gamepad.FlixelGamepadInputManager#ensureDevice(int) ensureDevice(int)}.
 * The manager guarantees at most one instance per slot:
 *
 * <pre>{@code
 * FlixelGamepadDevice pad = Flixel.gamepads.ensureDevice(0);
 * if (pad.isConnected()) {
 *   pad.vibrate(0.5f, 0.3f);
 * }
 * }</pre>
 *
 * <h2>Haptics and vibration</h2>
 *
 * <p>Vibration is routed through a
 * {@link org.flixelgdx.input.gamepad.FlixelGamepadHapticsProvider FlixelGamepadHapticsProvider}.
 * Each backend launcher installs the correct implementation automatically:
 *
 * <ul>
 *   <li>Desktop: Jamepad/SDL dual-rumble, with independent left (low-frequency) and right (high-frequency)
 *   motor channels.</li>
 *   <li>Web: W3C Gamepad Haptics API ({@code vibrationActuator.playEffect("dual-rumble", ...)}).
 *   Requires a Chromium-based browser; Firefox does not support {@code vibrationActuator}.</li>
 * </ul>
 *
 * <pre>{@code
 * // Full-strength rumble on both motors for 0.3 seconds.
 * Flixel.gamepads.vibrate(0, 0.3f);
 *
 * // Left motor only at half strength for 0.5 seconds.
 * Flixel.gamepads.vibrate(0, 0.5f, 0f, 0.5f);
 *
 * // Stop vibration immediately.
 * Flixel.gamepads.stopVibration(0);
 * }</pre>
 *
 * <p>For advanced platform-specific haptics (such as DualSense adaptive triggers), supply a custom
 * {@link org.flixelgdx.input.gamepad.FlixelGamepadHapticsProvider FlixelGamepadHapticsProvider}
 * via
 * {@link org.flixelgdx.input.gamepad.FlixelGamepadInputManager#setHapticsProvider(org.flixelgdx.input.gamepad.FlixelGamepadHapticsProvider)
 * setHapticsProvider(...)}.
 *
 * <h2>Dead zones</h2>
 *
 * <p>Analog sticks rarely return exactly zero at rest. Set
 * {@link org.flixelgdx.input.gamepad.FlixelGamepadInputManager#globalDeadZone globalDeadZone} to
 * filter out small resting values across all axes and slots:
 *
 * <pre>{@code
 * Flixel.gamepads.globalDeadZone = 0.15f; // Ignore values below 15% of full deflection.
 * }</pre>
 *
 * <p>Raw axis values (before dead zone) are accessible via the gamepad slot's
 * {@link org.flixelgdx.input.gamepad.FlixelGamepad FlixelGamepad} backend, reached through
 * {@link org.flixelgdx.input.gamepad.FlixelGamepadInputManager#gamepadAt(int) gamepadAt(int)}.
 *
 * <h2>Disabling the system</h2>
 *
 * <p>Set {@link org.flixelgdx.input.gamepad.FlixelGamepadInputManager#enabled enabled} to
 * {@code false} to skip all controller polling entirely for games that never use gamepads:
 *
 * <pre>{@code
 * Flixel.gamepads.enabled = false;
 * }</pre>
 */
package org.flixelgdx.input.gamepad;
