/**
 * The complete input layer for FlixelGDX, covering keyboards, mice, touch screens, gamepads,
 * and rebindable logical actions.
 *
 * <h2>Two-layer architecture</h2>
 *
 * <p>Input is structured in two layers that work together.
 *
 * <p>The <b>first layer</b> is {@link org.flixelgdx.input.FlixelInputDevice FlixelInputDevice},
 * the raw platform backend. Each backend (desktop, mobile, web) provides its own implementation
 * and registers it at startup. Game code rarely talks to this layer directly, but it is reachable
 * through {@link org.flixelgdx.Flixel#input Flixel.input} when you need low-level polling or
 * want to register a listener for raw events. A safe no-op default
 * ({@link org.flixelgdx.input.FlixelNoopInputDevice FlixelNoopInputDevice}) is installed before
 * any backend starts, so {@code Flixel.input} is never {@code null}.
 *
 * <p>The <b>second layer</b> is the set of polled managers that build richer state on top of that
 * backend. Each manager implements {@link org.flixelgdx.input.FlixelInputManager FlixelInputManager}
 * and provides familiar
 * {@code pressed}/{@code justPressed}/{@code justReleased} semantics:
 *
 * <ul>
 *   <li><b>Keyboard</b> - {@link org.flixelgdx.input.keyboard.FlixelKeyInputManager FlixelKeyInputManager}
 *       on {@link org.flixelgdx.Flixel#keys Flixel.keys}.</li>
 *   <li><b>Mouse</b> - {@link org.flixelgdx.input.mouse.FlixelMouseInputManager FlixelMouseInputManager}
 *       on {@link org.flixelgdx.Flixel#mouse Flixel.mouse}.</li>
 *   <li><b>Touch</b> - {@link org.flixelgdx.input.touch.FlixelTouchManager FlixelTouchManager}
 *       on {@link org.flixelgdx.Flixel#touches Flixel.touches}.</li>
 *   <li><b>Gamepads</b> - {@link org.flixelgdx.input.gamepad.FlixelGamepadInputManager FlixelGamepadInputManager}
 *       on {@link org.flixelgdx.Flixel#gamepads Flixel.gamepads}.</li>
 * </ul>
 *
 * <p>For rebindable controls (keyboard + gamepad sharing one action, Steam Input support), see the
 * {@link org.flixelgdx.input.action action} sub-package.
 *
 * <h2>Polling versus event callbacks</h2>
 *
 * <p>Input arrives two complementary ways.
 *
 * <p><b>Polling</b> is the typical path for game code. Ask "is this button held?" each frame and
 * act on the result directly:
 *
 * <pre>{@code
 * // In FlixelState.update(float elapsed):
 * if (Flixel.keys.pressed(FlixelKey.LEFT)) {
 *   player.setVelocityX(-100f);
 * }
 * if (Flixel.mouse.justPressed(FlixelMouseButton.LEFT)) {
 *   shoot(Flixel.mouse.getWorldX(), Flixel.mouse.getWorldY());
 * }
 * }</pre>
 *
 * <p><b>Event callbacks</b> let you react to input the moment it arrives. Register a
 * {@link org.flixelgdx.input.FlixelKeyboardListener}, {@link org.flixelgdx.input.FlixelMouseListener}, or
 * {@link org.flixelgdx.input.FlixelTouchListener} with the active {@link org.flixelgdx.input.FlixelInputDevice}
 * and the backend fires the relevant method for each raw event. This is useful for systems (such as text-entry fields)
 * that need every keystroke in order, or for low-latency touch processing that cannot wait until the next {@code update()}
 * call. Returning {@code true} from any callback method consumes the event and stops it from reaching
 * further listeners.
 *
 * <pre>{@code
 * Flixel.input.addKeyboardListener(new FlixelKeyboardListener() {
 *   @Override
 *   public boolean keyTyped(char character) {
 *     textField.append(character);
 *     return true; // Consume so game-level shortcuts don't also fire.
 *   }
 * });
 * }</pre>
 *
 * <h2>Frame contract</h2>
 *
 * <p>{@link org.flixelgdx.input.FlixelInputManager} has two lifecycle methods that every manager implements:
 *
 * <ul>
 *   <li>{@link org.flixelgdx.input.FlixelInputManager#update() update()} - called once near the <em>start</em> of
 *       each frame to read hardware state and refresh internal snapshots.</li>
 *   <li>{@link org.flixelgdx.input.FlixelInputManager#endFrame() endFrame()} - called once near the <em>end</em> of
 *       each frame (after game logic and rendering) to finalize edge-detection state so
 *       {@code justPressed()} and {@code justReleased()} stay valid for the whole frame.</li>
 * </ul>
 *
 * <p>{@link org.flixelgdx.FlixelGame FlixelGame} drives this automatically. Game code in
 * {@link org.flixelgdx.FlixelState#update(float) FlixelState.update(float)} runs after
 * {@code update()} and before {@code endFrame()}, so action queries always see a consistent
 * snapshot for the current frame.
 *
 * <h2>Screen coordinates</h2>
 *
 * <p>All raw pointer positions (mouse, touch, the getters on {@link org.flixelgdx.input.FlixelInputDevice}) use
 * screen-space pixels with the origin in the top-left corner: X grows right, Y grows down.
 * This matches raw window coordinates and differs from FlixelGDX world coordinates, whose origin
 * is bottom-left. Convert between the two through a
 * {@link org.flixelgdx.FlixelCamera FlixelCamera}; the mouse and touch managers do this
 * automatically when you read {@code getWorldX()} or
 * {@link org.flixelgdx.input.touch.FlixelTouch#worldX FlixelTouch.worldX}.
 *
 * <h2>Sub-packages</h2>
 *
 * <ul>
 *   <li>{@link org.flixelgdx.input.keyboard keyboard} - key codes and keyboard manager.</li>
 *   <li>{@link org.flixelgdx.input.mouse mouse} - button codes, mouse manager, and cursor styling.</li>
 *   <li>{@link org.flixelgdx.input.touch touch} - per-finger state and multitouch manager.</li>
 *   <li>{@link org.flixelgdx.input.gamepad gamepad} - gamepad manager, haptics, and mapping system.</li>
 *   <li>{@link org.flixelgdx.input.action action} - rebindable logical actions with Steam Input support.</li>
 * </ul>
 */
package org.flixelgdx.input;
